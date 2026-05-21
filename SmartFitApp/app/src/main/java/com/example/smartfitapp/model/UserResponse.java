package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    public Long id;
    @SerializedName("full_name")
    public String fullName;
    public String email;
    public String role;
    // Profile fields (null when not set)
    public String gender;
    @SerializedName("height_cm")
    public Integer heightCm;
    @SerializedName("weight_kg")
    public Double weightKg;
    @SerializedName("preferred_size_system")
    public String preferredSizeSystem;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("updated_at")
    public String updatedAt;
}
