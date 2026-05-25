package com.example.FitApp.fitting;

import com.example.FitApp.catalog.ClothingItem;
import com.example.FitApp.catalog.ClothingItemRepository;
import com.example.FitApp.fitting.dto.*;
import com.example.FitApp.image.Image;
import com.example.FitApp.image.ImageRepository;
import com.example.FitApp.tryon.TryOnResult;
import com.example.FitApp.tryon.TryOnResultRepository;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FittingResultService {

    private final FittingResultRepository fittingResultRepository;
    private final ImageRepository imageRepository;
    private final ClothingItemRepository clothingItemRepository;
    private final TryOnResultRepository tryOnResultRepository;
    private final ObjectMapper objectMapper;

    public FittingResultResponse create(User user, FittingResultRequest request) {
        if (request == null || request.getImageId() == null || request.getItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image_id and item_id are required");
        }

        Image image = imageRepository.findByIdAndUserId(request.getImageId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
        ClothingItem item = clothingItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clothing item not found"));

        TryOnResult tryOnResult = null;
        if (request.getTryonId() != null) {
            tryOnResult = tryOnResultRepository.findByIdAndUserId(request.getTryonId(), user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Try-on result not found"));
        }

        Summary summary = buildSummary(image, tryOnResult);
        FittingResult result = FittingResult.builder()
                .userId(user.getId())
                .imageId(image.getId())
                .itemId(item.getId())
                .tryonId(request.getTryonId())
                .fitStatus(summary.fitStatus())
                .fitLabel(summary.fitLabel())
                .confidenceScore(summary.confidenceScore())
                .summary(summary.summary())
                .warnings(serializeWarnings(summary.warnings()))
                .resultImageUrl(summary.resultImageUrl())
                .build();

        return toResponse(fittingResultRepository.save(result), item);
    }

    public FittingResultListResponse getMyResults(User user) {
        List<FittingResultResponse> items = fittingResultRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
        return FittingResultListResponse.builder().items(items).build();
    }

    public FittingResultResponse getResult(User user, Long resultId) {
        FittingResult result = fittingResultRepository.findByIdAndUserId(resultId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fitting result not found"));
        return toResponse(result);
    }

    @Transactional
    public void deleteResult(User user, Long resultId) {
        FittingResult result = fittingResultRepository.findByIdAndUserId(resultId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fitting result not found"));
        fittingResultRepository.delete(result);
    }

    private Summary buildSummary(Image image, TryOnResult tryOnResult) {
        Double confidence = tryOnResult != null ? tryOnResult.getConfidenceScore() : null;
        String resultImageUrl = tryOnResult != null ? tryOnResult.getResultImageUrl() : "/uploads/" + image.getFilename();

        String fitStatus = "uncertain";
        String fitLabel = "Uncertain Fit";
        String summary = "Not enough data is available to determine final fit.";

        if (tryOnResult != null) {
            fitStatus = "preview_only";
            fitLabel = "Preview Only";
            summary = "Virtual try-on preview generated.";
        }

        List<String> warnings = new ArrayList<>();
        if (tryOnResult != null) warnings.addAll(deserializeWarnings(tryOnResult.getWarnings()));
        warnings.add("Fitting results are based on the generated try-on preview only.");
        return new Summary(fitStatus, fitLabel, confidence, summary, warnings, resultImageUrl);
    }

    private String labelForStatus(String fitStatus) {
        return switch (fitStatus) {
            case "good_fit" -> "Recommended Fit";
            case "tight_fit" -> "Tight Fit";
            case "loose_fit" -> "Loose Fit";
            case "preview_only" -> "Preview Only";
            default -> "Uncertain Fit";
        };
    }

    private FittingResultResponse toResponse(FittingResult result) {
        ClothingItem item = clothingItemRepository.findById(result.getItemId()).orElse(null);
        return toResponse(result, item);
    }

    private FittingResultResponse toResponse(FittingResult result, ClothingItem item) {
        return FittingResultResponse.builder()
                .id(result.getId())
                .imageId(result.getImageId())
                .itemId(result.getItemId())
                .tryonId(result.getTryonId())
                .fitStatus(result.getFitStatus())
                .fitLabel(result.getFitLabel())
                .confidenceScore(result.getConfidenceScore())
                .summary(result.getSummary())
                .warnings(deserializeWarnings(result.getWarnings()))
                .resultImageUrl(result.getResultImageUrl())
                .createdAt(result.getCreatedAt())
                .clothingItem(toClothingItemResponse(item))
                .build();
    }

    private FittingClothingItemResponse toClothingItemResponse(ClothingItem item) {
        if (item == null) return null;
        return FittingClothingItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .imageUrl(item.getImageUrl())
                .build();
    }

    private String serializeWarnings(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) return null;
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

    private record Summary(String fitStatus, String fitLabel, Double confidenceScore,
                           String summary, List<String> warnings, String resultImageUrl) {}
}
