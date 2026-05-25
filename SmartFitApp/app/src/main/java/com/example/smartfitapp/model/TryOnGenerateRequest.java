package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class TryOnGenerateRequest {
    @SerializedName("image_id")
    public Long imageId;

    @SerializedName("item_id")
    public Long itemId;

    public TryOnGenerateRequest(Long imageId, Long itemId) {
        this.imageId = imageId;
        this.itemId = itemId;
    }
}
