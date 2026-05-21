package com.example.FitApp.catalog.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClothingItemSaveResponse {
    private String message;
    private ClothingItemResponse item;
}
