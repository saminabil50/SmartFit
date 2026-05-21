package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserPreferences {
    public Long id;
    @SerializedName("user_id")
    public Long userId;
    @SerializedName("preferred_size_system")
    public String preferredSizeSystem;
    @SerializedName("preferred_fit")
    public String preferredFit;
    @SerializedName("preferred_gender_category")
    public String preferredGenderCategory;
    @SerializedName("preferred_categories")
    public List<String> preferredCategories;
    @SerializedName("default_image_type")
    public String defaultImageType;
    @SerializedName("camera_enabled")
    public Boolean cameraEnabled;
    @SerializedName("save_uploaded_images")
    public Boolean saveUploadedImages;
    @SerializedName("save_measurement_history")
    public Boolean saveMeasurementHistory;
    @SerializedName("save_tryon_history")
    public Boolean saveTryonHistory;
    @SerializedName("data_usage_consent")
    public Boolean dataUsageConsent;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("updated_at")
    public String updatedAt;
}
