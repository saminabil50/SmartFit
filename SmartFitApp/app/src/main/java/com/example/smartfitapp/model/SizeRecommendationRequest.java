package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class SizeRecommendationRequest {
    @SerializedName("measurement_id")
    public Long measurementId;

    @SerializedName("item_id")
    public Long itemId;

    @SerializedName("fit_preference")
    public String fitPreference;

    public SizeRecommendationRequest(Long measurementId, Long itemId, String fitPreference) {
        this.measurementId = measurementId;
        this.itemId = itemId;
        this.fitPreference = fitPreference;
    }
}
