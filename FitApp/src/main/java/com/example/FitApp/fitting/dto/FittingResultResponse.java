package com.example.FitApp.fitting.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class FittingResultResponse {
    private Long id;
    private Long imageId;
    private Long itemId;
    private Long measurementId;
    private Long recommendationId;
    private Long tryonId;
    private String recommendedSize;
    private String fitStatus;
    private String fitLabel;
    private Double confidenceScore;
    private String summary;
    private List<String> warnings;
    private String resultImageUrl;
    private LocalDateTime createdAt;
    private FittingMeasurementSummaryResponse measurementSummary;
    private FittingRecommendationSummaryResponse recommendation;
    private FittingClothingItemResponse clothingItem;
}
