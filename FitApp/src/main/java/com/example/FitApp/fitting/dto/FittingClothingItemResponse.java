package com.example.FitApp.fitting.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FittingClothingItemResponse {
    private Long id;
    private String name;
    private String category;
    private String imageUrl;
}
