package com.example.FitApp.catalog.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ClothingItemRequest {
    private String name;
    private String category;
    private String gender;
    private List<String> availableSizes;
    private String imageUrl;
    private Map<String, Object> sizeChart;  // accepted as JSON object
}
