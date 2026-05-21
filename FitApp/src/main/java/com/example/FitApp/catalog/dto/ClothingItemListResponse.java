package com.example.FitApp.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClothingItemListResponse {
    private List<ClothingItemResponse> items;
    private long total;
    private int page;
    private int limit;
}
