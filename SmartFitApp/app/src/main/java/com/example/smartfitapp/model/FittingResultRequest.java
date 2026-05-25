package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class FittingResultRequest {
    @SerializedName("image_id") public Long imageId;
    @SerializedName("item_id") public Long itemId;
    @SerializedName("tryon_id") public Long tryonId;

    public FittingResultRequest(Long imageId, Long itemId, Long tryonId) {
        this.imageId = imageId;
        this.itemId = itemId;
        this.tryonId = tryonId;
    }
}
