package com.example.FitApp.tryon;

import com.example.FitApp.ai.AiClient;
import com.example.FitApp.catalog.ClothingItem;
import com.example.FitApp.catalog.ClothingItemRepository;
import com.example.FitApp.image.Image;
import com.example.FitApp.image.ImageRepository;
import com.example.FitApp.image.ImageService;
import com.example.FitApp.image.dto.ImageResponse;
import com.example.FitApp.tryon.dto.TryOnClothingItemResponse;
import com.example.FitApp.tryon.dto.TryOnGenerateRequest;
import com.example.FitApp.tryon.dto.TryOnResultListResponse;
import com.example.FitApp.tryon.dto.TryOnResultResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TryOnService {

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    private final TryOnResultRepository tryOnRepository;
    private final ImageRepository imageRepository;
    private final ImageService imageService;
    private final ClothingItemRepository clothingItemRepository;
    private final ObjectMapper objectMapper;
    private final AiClient aiClient;

    public TryOnResultResponse generate(User user, TryOnGenerateRequest request) {
        if (request == null || request.getImageId() == null || request.getItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image_id and item_id are required");
        }

        Image image = imageRepository.findByIdAndUserId(request.getImageId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        ClothingItem item = clothingItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found"));

        AiTryOnResult aiResult = generateWithAi(image, item);

        TryOnResult result = TryOnResult.builder()
                .userId(user.getId())
                .imageId(image.getId())
                .itemId(item.getId())
                .resultImageUrl(aiResult.resultImageUrl())
                .status(aiResult.status())
                .confidenceScore(aiResult.confidenceScore())
                .warnings(serializeWarnings(aiResult.warnings()))
                .build();

        return toResponse(tryOnRepository.save(result), item);
    }

    public TryOnResultResponse generateFromImage(User user, MultipartFile file, Long itemId) {
        ImageResponse image = imageService.upload(user, file, null);
        TryOnGenerateRequest request = new TryOnGenerateRequest();
        request.setImageId(image.getId());
        request.setItemId(itemId);
        return generate(user, request);
    }

    @SuppressWarnings("unchecked")
    private AiTryOnResult generateWithAi(Image image, ClothingItem item) {
        String clothingImagePath = resolveClothingImagePath(item);
        Path outputDir = Paths.get(uploadsDir).toAbsolutePath().normalize().resolve("tryon");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to prepare try-on output directory");
        }

        Map<String, Object> response = aiClient.generateTryOn(
                image.getFilePath(),
                clothingImagePath,
                outputDir.toString()
        );
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI try-on server is unavailable");
        }
        if (response.containsKey("_ai_error_body")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    extractAiErrorDetail(stringValue(response.get("_ai_error_body")))
            );
        }

        // Prefer result_image_url (fal.ai CDN URL), fall back to result_image_path (local compositing)
        String resultImageUrl = stringValue(response.get("result_image_url"));
        if (resultImageUrl == null || resultImageUrl.isBlank()) {
            String resultImagePath = stringValue(response.get("result_image_path"));
            if (resultImagePath == null || resultImagePath.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI try-on did not return a result image");
            }
            resultImageUrl = toUploadsUrl(resultImagePath);
        }

        String status = stringValue(response.get("status"));
        Number confidence = response.get("confidence_score") instanceof Number number ? number : null;
        List<String> warnings = response.get("warnings") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : Collections.emptyList();

        log.info("AI try-on completed for image {} item {} status {} url {}",
                image.getId(), item.getId(), status, resultImageUrl);
        return new AiTryOnResult(
                resultImageUrl,
                status == null || status.isBlank() ? "completed" : status,
                confidence != null ? confidence.doubleValue() : null,
                warnings
        );
    }

    private String resolveClothingImagePath(ClothingItem item) {
        String imageUrl = item.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected clothing item does not have an image");
        }
        if (!imageUrl.startsWith("/uploads/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected clothing item image must be a local uploaded image");
        }

        Path uploadsPath = Paths.get(uploadsDir).toAbsolutePath().normalize();
        Path imagePath = uploadsPath.resolve(imageUrl.substring("/uploads/".length())).normalize();
        if (!imagePath.startsWith(uploadsPath) || !Files.exists(imagePath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected clothing item image file was not found");
        }
        return imagePath.toString();
    }

    private String toUploadsUrl(String resultImagePath) {
        Path uploadsPath = Paths.get(uploadsDir).toAbsolutePath().normalize();
        Path resultPath = Paths.get(resultImagePath).toAbsolutePath().normalize();

        if (!Files.exists(resultPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI try-on result image file was not found");
        }

        if (resultPath.startsWith(uploadsPath)) {
            String relative = uploadsPath.relativize(resultPath).toString().replace('\\', '/');
            return "/uploads/" + relative;
        }

        Path tryOnDir = uploadsPath.resolve("tryon").normalize();
        try {
            Files.createDirectories(tryOnDir);
            String filename = "ai-" + System.currentTimeMillis() + "-" + resultPath.getFileName();
            Path copied = tryOnDir.resolve(filename).normalize();
            Files.copy(resultPath, copied);
            return "/uploads/tryon/" + filename;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to store AI try-on result image");
        }
    }

    public TryOnResultListResponse getMyResults(User user) {
        List<TryOnResultResponse> items = tryOnRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
        return TryOnResultListResponse.builder().items(items).build();
    }

    public TryOnResultResponse getResult(User user, Long tryOnId) {
        TryOnResult result = tryOnRepository.findByIdAndUserId(tryOnId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Try-on result not found"));
        return toResponse(result);
    }

    @Transactional
    public void deleteResult(User user, Long tryOnId) {
        TryOnResult result = tryOnRepository.findByIdAndUserId(tryOnId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Try-on result not found"));
        deleteGeneratedFileIfOwned(result.getResultImageUrl());
        tryOnRepository.delete(result);
    }

    private TryOnResultResponse toResponse(TryOnResult result) {
        ClothingItem item = clothingItemRepository.findById(result.getItemId()).orElse(null);
        return toResponse(result, item);
    }

    private TryOnResultResponse toResponse(TryOnResult result, ClothingItem item) {
        return TryOnResultResponse.builder()
                .id(result.getId())
                .imageId(result.getImageId())
                .itemId(result.getItemId())
                .status(result.getStatus())
                .resultImageUrl(result.getResultImageUrl())
                .confidenceScore(result.getConfidenceScore())
                .warnings(deserializeWarnings(result.getWarnings()))
                .createdAt(result.getCreatedAt())
                .clothingItem(toClothingItemResponse(item))
                .build();
    }

    private TryOnClothingItemResponse toClothingItemResponse(ClothingItem item) {
        if (item == null) return null;
        return TryOnClothingItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .imageUrl(item.getImageUrl())
                .build();
    }

    private String serializeWarnings(List<String> warnings) {
        try {
            return objectMapper.writeValueAsString(warnings);
        } catch (JacksonException e) {
            return null;
        }
    }

    private List<String> deserializeWarnings(String warnings) {
        if (warnings == null || warnings.isBlank()) return List.of();
        try {
            return objectMapper.readValue(warnings, new TypeReference<>() {});
        } catch (JacksonException e) {
            return List.of(warnings);
        }
    }

    private void deleteGeneratedFileIfOwned(String resultImageUrl) {
        if (resultImageUrl == null || !resultImageUrl.startsWith("/uploads/tryon/")) return;

        Path uploadsPath = Paths.get(uploadsDir).toAbsolutePath().normalize();
        Path tryOnPath = uploadsPath.resolve("tryon").normalize();
        Path target = uploadsPath.resolve(resultImageUrl.substring("/uploads/".length())).normalize();
        if (!target.startsWith(tryOnPath)) return;

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Missing generated files should not block deleting the database record.
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String extractAiErrorDetail(String body) {
        if (body == null || body.isBlank()) {
            return "AI try-on failed";
        }
        try {
            Object parsed = objectMapper.readValue(body, Object.class);
            if (parsed instanceof Map<?, ?> map && map.get("detail") != null) {
                return String.valueOf(map.get("detail"));
            }
        } catch (Exception ignored) {
            // Fall through to raw body below.
        }
        return body;
    }

    private record AiTryOnResult(String resultImageUrl, String status, Double confidenceScore, List<String> warnings) {}
}
