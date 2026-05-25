package com.example.FitApp.tryon.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TryOnResultResponse {
    private Long id;
    private Long imageId;
    private Long itemId;
    private String status;
    private String resultImageUrl;
    private Double confidenceScore;
    private List<String> warnings;
    private LocalDateTime createdAt;
    private TryOnClothingItemResponse clothingItem;
}
