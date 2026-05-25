package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TryOnResult {
    @SerializedName("id") public Long id;
    @SerializedName("image_id") public Long imageId;
    @SerializedName("item_id") public Long itemId;
    @SerializedName("status") public String status;
    @SerializedName("result_image_url") public String resultImageUrl;
    @SerializedName("confidence_score") public Double confidenceScore;
    @SerializedName("warnings") public List<String> warnings;
    @SerializedName("created_at") public String createdAt;
    @SerializedName("clothing_item") public ClothingItem clothingItem;
}
