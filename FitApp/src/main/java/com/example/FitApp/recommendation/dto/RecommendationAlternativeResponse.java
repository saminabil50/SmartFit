package com.example.FitApp.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendationAlternativeResponse {
    private String size;
    private String fitType;
    private Double confidenceScore;
    private String reason;
}
