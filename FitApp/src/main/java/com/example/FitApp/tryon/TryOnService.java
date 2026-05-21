package com.example.FitApp.tryon;

import com.example.FitApp.catalog.ClothingItem;
import com.example.FitApp.catalog.ClothingItemRepository;
import com.example.FitApp.image.Image;
import com.example.FitApp.image.ImageRepository;
import com.example.FitApp.measurement.MeasurementRepository;
import com.example.FitApp.preferences.PreferencesService;
import com.example.FitApp.tryon.dto.TryOnClothingItemResponse;
import com.example.FitApp.tryon.dto.TryOnGenerateRequest;
import com.example.FitApp.tryon.dto.TryOnResultListResponse;
import com.example.FitApp.tryon.dto.TryOnResultResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TryOnService {

    private static final String COMPLETED = "completed";
    private static final List<String> MVP_WARNINGS = List.of(
            "MVP placeholder preview. Real clothing overlay will be implemented later.",
            "MVP preview is approximate and may not represent exact real-world fit."
    );

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    private final TryOnResultRepository tryOnRepository;
    private final ImageRepository imageRepository;
    private final ClothingItemRepository clothingItemRepository;
    private final MeasurementRepository measurementRepository;
    private final PreferencesService preferencesService;
    private final ObjectMapper objectMapper;

    public TryOnResultResponse generate(User user, TryOnGenerateRequest request) {
        if (request == null || request.getImageId() == null || request.getItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image_id and item_id are required");
        }

        Image image = imageRepository.findByIdAndUserId(request.getImageId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        ClothingItem item = clothingItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found"));

        if (request.getMeasurementId() != null) {
            measurementRepository.findByIdAndUserId(request.getMeasurementId(), user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Measurement not found"));
        }

        TryOnResult result = TryOnResult.builder()
                .userId(user.getId())
                .imageId(image.getId())
                .itemId(item.getId())
                .measurementId(request.getMeasurementId())
                .resultImageUrl("/uploads/" + image.getFilename())
                .status(COMPLETED)
                .confidenceScore(0.50)
                .warnings(serializeWarnings(MVP_WARNINGS))
                .build();

        if (!preferencesService.shouldSaveTryonHistory(user)) {
            return toResponse(result, item);
        }

        return toResponse(tryOnRepository.save(result), item);
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
                .measurementId(result.getMeasurementId())
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
}
