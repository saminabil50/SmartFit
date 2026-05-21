package com.example.FitApp.catalog.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ClothingItemRequest {
    private String name;
    private String description;
    private String category;
    private String gender;
    private String brand;
    private String sizeSystem;
    private List<String> availableSizes;
    private Double basePrice;
    private String currency;
    private String imageUrl;
    private Boolean isActive;
    private Map<String, Object> sizeChart;  // accepted as JSON object
}
