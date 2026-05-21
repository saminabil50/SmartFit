package com.example.FitApp.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendationClothingItemResponse {
    private Long id;
    private String name;
    private String category;
    private String imageUrl;
}
