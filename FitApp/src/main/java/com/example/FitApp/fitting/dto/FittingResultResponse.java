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
    private Long tryonId;
    private String fitStatus;
    private String fitLabel;
    private Double confidenceScore;
    private String summary;
    private List<String> warnings;
    private String resultImageUrl;
    private LocalDateTime createdAt;
    private FittingClothingItemResponse clothingItem;
}
