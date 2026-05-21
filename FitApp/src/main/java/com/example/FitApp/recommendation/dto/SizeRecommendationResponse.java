package com.example.FitApp.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SizeRecommendationResponse {
    private Long id;
    private Long measurementId;
    private Long itemId;
    private String recommendedSize;
    private String fitPreference;
    private String fitType;
    private Double confidenceScore;
    private String reason;
    private List<RecommendationAlternativeResponse> alternatives;
    private LocalDateTime createdAt;
    private RecommendationClothingItemResponse clothingItem;
}
