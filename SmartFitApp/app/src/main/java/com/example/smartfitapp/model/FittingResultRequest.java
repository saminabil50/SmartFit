package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class FittingResultRequest {
    @SerializedName("image_id") public Long imageId;
    @SerializedName("item_id") public Long itemId;
    @SerializedName("measurement_id") public Long measurementId;
    @SerializedName("recommendation_id") public Long recommendationId;
    @SerializedName("tryon_id") public Long tryonId;

    public FittingResultRequest(Long imageId, Long itemId, Long measurementId, Long recommendationId, Long tryonId) {
        this.imageId = imageId;
        this.itemId = itemId;
        this.measurementId = measurementId;
        this.recommendationId = recommendationId;
        this.tryonId = tryonId;
    }
}
