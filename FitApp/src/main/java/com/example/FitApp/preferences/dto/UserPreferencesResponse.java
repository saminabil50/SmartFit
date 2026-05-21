package com.example.FitApp.preferences.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserPreferencesResponse {
    private Long id;
    private Long userId;
    private String preferredSizeSystem;
    private String preferredFit;
    private String preferredGenderCategory;
    private List<String> preferredCategories;
    private String defaultImageType;
    private Boolean cameraEnabled;
    private Boolean saveUploadedImages;
    private Boolean saveMeasurementHistory;
    private Boolean saveTryonHistory;
    private Boolean dataUsageConsent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
