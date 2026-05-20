package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class UpdateProfileRequest {
    @SerializedName("full_name")
    public String fullName;
    public String gender;
    @SerializedName("height_cm")
    public Integer heightCm;
    @SerializedName("weight_kg")
    public Double weightKg;
    @SerializedName("preferred_size_system")
    public String preferredSizeSystem;
}
