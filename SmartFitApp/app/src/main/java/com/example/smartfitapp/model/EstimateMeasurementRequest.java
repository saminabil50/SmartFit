package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class EstimateMeasurementRequest {
    @SerializedName("image_id") public Long imageId;
    @SerializedName("height_cm") public Integer heightCm;

    public EstimateMeasurementRequest(Long imageId, Integer heightCm) {
        this.imageId = imageId;
        this.heightCm = heightCm;
    }
}
