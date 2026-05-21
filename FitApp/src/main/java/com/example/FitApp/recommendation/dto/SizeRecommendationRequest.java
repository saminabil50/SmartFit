package com.example.FitApp.recommendation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SizeRecommendationRequest {
    private Long measurementId;
    private Long itemId;
    private String fitPreference;
}
