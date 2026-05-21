package com.example.FitApp.fitting.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FittingRecommendationSummaryResponse {
    private String recommendedSize;
    private String fitPreference;
    private Double confidenceScore;
    private String reason;
}
