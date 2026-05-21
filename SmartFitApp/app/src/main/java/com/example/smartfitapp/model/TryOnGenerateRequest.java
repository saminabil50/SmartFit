package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class TryOnGenerateRequest {
    @SerializedName("image_id")
    public Long imageId;

    @SerializedName("item_id")
    public Long itemId;

    @SerializedName("measurement_id")
    public Long measurementId;

    public TryOnGenerateRequest(Long imageId, Long itemId, Long measurementId) {
        this.imageId = imageId;
        this.itemId = itemId;
        this.measurementId = measurementId;
    }
}
