package com.example.FitApp.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClothingItemResponse {
    private Long id;
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
    @JsonRawValue
    private String sizeChart;      // stored JSON returned verbatim as JSON object
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
