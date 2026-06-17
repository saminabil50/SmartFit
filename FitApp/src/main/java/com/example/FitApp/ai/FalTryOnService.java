package com.example.FitApp.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class FalTryOnService {

    private static final String FAL_QUEUE_BASE_URL = "https://queue.fal.run";
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("tops", "bottoms", "one-pieces", "auto");
    private static final Set<String> ALLOWED_MODES = Set.of("performance", "balanced", "quality");
    private static final Set<String> ALLOWED_GARMENT_PHOTO_TYPES = Set.of("auto", "model", "flat-lay");
    private static final Set<String> ALLOWED_MODERATION_LEVELS = Set.of("none", "permissive", "conservative");
    private static final Set<String> ALLOWED_OUTPUT_FORMATS = Set.of("png", "jpeg");
    private static final int MAX_NUM_SAMPLES = 2;

    private final ObjectMapper objectMapper;
    private final RestClient falRestClient;

    public FalTryOnService(
            ObjectMapper objectMapper,
            @Value("${fal.http.connect-timeout-seconds:30}") long connectTimeoutSeconds,
            @Value("${fal.http.read-timeout-seconds:300}") long readTimeoutSeconds
    ) {
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        this.falRestClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Value("${fal.key:}")
    private String falKey;

    @Value("${fal.tryon.model:fal-ai/fashn/tryon/v1.5}")
    private String model;

    @Value("${fal.tryon.category:auto}")
    private String category;

    @Value("${fal.tryon.mode:quality}")
    private String mode;

    @Value("${fal.tryon.garment-photo-type:auto}")
    private String garmentPhotoType;

    @Value("${fal.tryon.moderation-level:permissive}")
    private String moderationLevel;

    @Value("${fal.tryon.num-samples:1}")
    private int numSamples;

    @Value("${fal.tryon.segmentation-free:true}")
    private boolean segmentationFree;

    @Value("${fal.tryon.output-format:png}")
    private String outputFormat;

    @Value("${fal.tryon.max-wait-seconds:360}")
    private long maxWaitSeconds;

    @Value("${fal.tryon.poll-interval-seconds:5}")
    private long pollIntervalSeconds;

    public boolean isConfigured() {
        return falKey != null && !falKey.isBlank();
    }

    public String getModel() {
        return model;
    }

    public FalTryOnResult generate(String userImagePath, String clothingImagePath) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FAL_KEY is not configured");
        }

        Map<String, Object> input = new HashMap<>();
        input.put("model_image", prepareFalInput(userImagePath));
        input.put("garment_image", prepareFalInput(clothingImagePath));
        input.putAll(defaultArguments());

        try {
            log.info("Calling fal.ai try-on model {} arguments {}", model, sanitizeForLogging(input));
            Map<String, Object> result = generateViaQueue(input);
            return toTryOnResult(result);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "fal.ai try-on failed: interrupted");
        } catch (Exception e) {
            log.error("fal.ai try-on failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "fal.ai try-on failed: " + rootMessage(e));
        }
    }

    private Map<String, Object> generateViaQueue(Map<String, Object> input) throws InterruptedException {
        String submitUrl = FAL_QUEUE_BASE_URL + "/" + model;
        Map<String, Object> submitted = executeFalJson(HttpMethod.POST, submitUrl, input);

        String requestId = stringValue(submitted.get("request_id"));
        String statusUrl = stringValue(submitted.get("status_url"));
        String responseUrl = stringValue(submitted.get("response_url"));
        if (requestId == null || statusUrl == null || responseUrl == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "fal.ai queue submit response is missing request_id/status_url/response_url"
            );
        }

        log.info(
                "fal.ai try-on queued request {} position {} status_url={} response_url={}",
                requestId,
                submitted.get("queue_position"),
                statusUrl,
                responseUrl
        );

        waitForCompletion(requestId, statusUrl);
        return executeFalJson(HttpMethod.GET, responseUrl, null);
    }

    private void waitForCompletion(String requestId, String statusUrl) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(Math.max(1, maxWaitSeconds)));
        long pollMillis = Duration.ofSeconds(Math.max(1, pollIntervalSeconds)).toMillis();
        int emittedLogCount = 0;

        while (Instant.now().isBefore(deadline)) {
            String separator = statusUrl.contains("?") ? "&" : "?";
            Map<String, Object> statusResponse = executeFalJson(HttpMethod.GET, statusUrl + separator + "logs=1", null);

            emittedLogCount = logProviderMessages(statusResponse, emittedLogCount);
            String status = stringValue(statusResponse.get("status"));
            if ("COMPLETED".equals(status)) {
                return;
            }

            log.info("fal.ai try-on request {} status {}", requestId, status);
            Thread.sleep(pollMillis);
        }

        throw new ResponseStatusException(
                HttpStatus.GATEWAY_TIMEOUT,
                "fal.ai try-on timed out after " + maxWaitSeconds + " seconds"
        );
    }

    private int logProviderMessages(Map<String, Object> statusResponse, int alreadyEmitted) {
        List<?> logs = statusResponse.get("logs") instanceof List<?> list ? list : null;
        if (logs == null || logs.size() <= alreadyEmitted) {
            return alreadyEmitted;
        }

        for (int i = alreadyEmitted; i < logs.size(); i++) {
            log.info("fal.ai try-on provider log {}", logs.get(i));
        }
        return logs.size();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeFalJson(HttpMethod method, String url, Map<String, Object> body) {
        String requestBody = body == null ? null : toJson(body);
        Instant startedAt = Instant.now();

        log.info("fal.ai request {} {}", method, url);
        log.info("fal.ai request headers {}", sanitizedRequestHeaders());
        if (requestBody != null) {
            log.info("fal.ai request body {}", sanitizeJsonForLogging(requestBody));
        }

        try {
            RestClient.RequestBodySpec requestSpec = falRestClient
                    .method(method)
                    .uri(url)
                    .headers(headers -> {
                        headers.set(HttpHeaders.AUTHORIZATION, "Key " + falKey.trim());
                        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                        if (requestBody != null) {
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        }
                    });
            if (requestBody != null) {
                requestSpec.body(requestBody);
            }
            return requestSpec.exchange((request, response) -> parseFalResponse(startedAt, response));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.error("fal.ai request {} {} failed after {}ms", method, url, durationMs, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "fal.ai request failed: " + rootMessage(e));
        }
    }

    private Map<String, Object> parseFalResponse(Instant startedAt, ClientHttpResponse response) throws IOException {
        String responseBody = readResponseBody(response);
        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info("fal.ai response {} {} after {}ms", response.getStatusCode().value(), response.getStatusText(), durationMs);
        log.info("fal.ai response headers {}", response.getHeaders());
        log.info("fal.ai response body {}", responseBody);

        if (response.getStatusCode().isError()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "fal.ai HTTP " + response.getStatusCode().value() + ": " + responseBody
            );
        }
        if (responseBody == null || responseBody.isBlank()) {
            return Collections.emptyMap();
        }
        return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
    }

    private Map<String, String> sanitizedRequestHeaders() {
        return Map.of(
                HttpHeaders.AUTHORIZATION, "Key <redacted>",
                HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE,
                HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE
        );
    }

    private String readResponseBody(ClientHttpResponse response) throws IOException {
        byte[] bytes = response.getBody().readAllBytes();
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize fal.ai request body");
        }
    }

    private String sanitizeJsonForLogging(String json) {
        return json
                .replaceAll("\"model_image\"\\s*:\\s*\"data:([^\"]{0,80})[^\"]*\"", "\"model_image\":\"data:$1...<redacted>\"")
                .replaceAll("\"garment_image\"\\s*:\\s*\"data:([^\"]{0,80})[^\"]*\"", "\"garment_image\":\"data:$1...<redacted>\"");
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> defaultArguments() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("category", validatedChoice(category, ALLOWED_CATEGORIES, "category"));
        arguments.put("mode", validatedChoice(mode, ALLOWED_MODES, "mode"));
        arguments.put("garment_photo_type", validatedChoice(
                garmentPhotoType,
                ALLOWED_GARMENT_PHOTO_TYPES,
                "garment_photo_type"
        ));
        arguments.put("moderation_level", validatedChoice(
                moderationLevel,
                ALLOWED_MODERATION_LEVELS,
                "moderation_level"
        ));
        arguments.put("num_samples", validatedNumSamples(numSamples));
        arguments.put("segmentation_free", segmentationFree);
        arguments.put("output_format", validatedChoice(outputFormat, ALLOWED_OUTPUT_FORMATS, "output_format"));
        return arguments;
    }

    private String prepareFalInput(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image path is required");
        }
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:")) {
            return value;
        }

        Path path = Path.of(value).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image file not found: " + value);
        }

        try {
            String mimeType = Files.probeContentType(path);
            if (mimeType == null || !mimeType.startsWith("image/")) {
                mimeType = "image/jpeg";
            }
            String encoded = Base64.getEncoder().encodeToString(Files.readAllBytes(path));
            return "data:" + mimeType + ";base64," + encoded;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read image file");
        }
    }

    private FalTryOnResult toTryOnResult(Map<String, Object> result) {
        String providerError = extractProviderError(result);
        if (providerError != null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, providerError);
        }

        Object imagesValue = result == null ? null : result.get("images");
        if (!(imagesValue instanceof List<?> images) || images.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "fal.ai try-on returned no images");
        }

        String imageUrl = extractImageUrl(images.get(0));
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "fal.ai try-on image has no URL");
        }

        return new FalTryOnResult(
                imageUrl,
                "completed",
                "fal.ai",
                model,
                null,
                List.of(),
                new ArrayList<>(images)
        );
    }

    private String extractImageUrl(Object image) {
        if (image instanceof Map<?, ?> map && map.get("url") != null) {
            return String.valueOf(map.get("url"));
        }
        return null;
    }

    private String extractProviderError(Map<String, Object> result) {
        if (result == null) {
            return null;
        }
        Object error = firstNonNull(result.get("error"), result.get("detail"), result.get("message"));
        if (error instanceof Map<?, ?> map) {
            Object message = firstNonNull(map.get("message"), map.get("detail"));
            return message == null ? String.valueOf(error) : String.valueOf(message);
        }
        return error == null ? null : String.valueOf(error);
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String validatedChoice(String value, Set<String> allowed, String field) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid " + field);
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid " + field + ". Allowed values: " + String.join(", ", allowed)
            );
        }
        return normalized;
    }

    private int validatedNumSamples(int value) {
        if (value < 1 || value > MAX_NUM_SAMPLES) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "num_samples must be between 1 and " + MAX_NUM_SAMPLES
            );
        }
        return value;
    }

    private Map<String, Object> sanitizeForLogging(Map<String, Object> input) {
        Map<String, Object> sanitized = new HashMap<>(input);
        sanitized.put("model_image", "<image>");
        sanitized.put("garment_image", "<image>");
        return sanitized;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
