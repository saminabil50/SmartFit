package com.example.FitApp.preferences.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserPreferencesRequest {
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
}
