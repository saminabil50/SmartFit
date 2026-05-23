package com.example.FitApp.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP client for calling the Python AI server (ai_server/).
 *
 * Configure the URL via:  AI_SERVER_URL=http://127.0.0.1:9000
 * or application.properties:  ai.server.url=http://127.0.0.1:9000
 *
 * All methods return null when the AI server is unreachable so callers can
 * fall back to existing backend logic without crashing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiClient {

    @Value("${ai.server.url:http://127.0.0.1:9000}")
    private String aiServerUrl;

    private final RestClient aiRestClient;

    public String getAiServerUrl() {
        return aiServerUrl;
    }

    /** GET /ai/health */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkAiHealth() {
        try {
            return aiRestClient.get()
                    .uri(aiServerUrl + "/ai/health")
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.warn("AI server health check failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * POST /ai/measurements/estimate
     *
     * @param imagePath  absolute or relative path to the user image
     * @param heightCm   optional height in cm (50–250); null falls back to 170 on AI side
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> estimateMeasurements(String imagePath, Double heightCm) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("image_path", imagePath);
            if (heightCm != null) body.put("height_cm", heightCm);

            return aiRestClient.post()
                    .uri(aiServerUrl + "/ai/measurements/estimate")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.warn("AI measurement estimation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * POST /ai/tryon/generate
     *
     * @param userImagePath     path to the user photo
     * @param clothingImagePath path to the clothing item image
     * @param outputDir         optional directory for the result image
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateTryOn(String userImagePath, String clothingImagePath, String outputDir) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("user_image_path", userImagePath);
            body.put("clothing_image_path", clothingImagePath);
            if (outputDir != null) body.put("output_dir", outputDir);

            return aiRestClient.post()
                    .uri(aiServerUrl + "/ai/tryon/generate")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.warn("AI try-on generation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * POST /ai/images/validate
     *
     * @param imagePath path to the image to validate
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> validateImage(String imagePath) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("image_path", imagePath);

            return aiRestClient.post()
                    .uri(aiServerUrl + "/ai/images/validate")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.warn("AI image validation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * POST /ai/images/remove-background
     *
     * Removes the background from a clothing/catalog image using rembg (U²-Net).
     * The first call may be slow (~30-120 s) due to model download.
     * Subsequent calls are typically 1-5 s on CPU.
     *
     * @param imagePath absolute path to the source image
     * @param outputDir optional absolute path for the output PNG; AI server default used when null
     * @return parsed JSON with output_image_path, status, warnings — or null on failure
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> removeBackground(String imagePath, String outputDir) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("image_path", imagePath);
            if (outputDir != null) body.put("output_dir", outputDir);

            return aiRestClient.post()
                    .uri(aiServerUrl + "/ai/images/remove-background")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.warn("AI background removal failed: {}", e.getMessage());
            return null;
        }
    }
}
