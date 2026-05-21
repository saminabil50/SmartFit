package com.example.FitApp.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SizeRecommendationListResponse {
    private List<SizeRecommendationResponse> items;
}
