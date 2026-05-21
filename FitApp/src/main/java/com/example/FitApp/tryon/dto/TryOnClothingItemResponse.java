package com.example.FitApp.tryon.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TryOnClothingItemResponse {
    private Long id;
    private String name;
    private String category;
    private String imageUrl;
}
