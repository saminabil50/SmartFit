package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class MeasurementResponse {
    @SerializedName("id") public Long id;
    @SerializedName("image_id") public Long imageId;
    @SerializedName("height_cm_used") public Integer heightCmUsed;
    @SerializedName("shoulder_width") public Double shoulderWidth;
    @SerializedName("chest") public Double chest;
    @SerializedName("waist") public Double waist;
    @SerializedName("hip") public Double hip;
    @SerializedName("inseam") public Double inseam;
    @SerializedName("confidence_score") public Double confidenceScore;
    @SerializedName("created_at") public String createdAt;
}
