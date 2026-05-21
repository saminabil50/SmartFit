package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SizeRecommendation {
    @SerializedName("id") public Long id;
    @SerializedName("measurement_id") public Long measurementId;
    @SerializedName("item_id") public Long itemId;
    @SerializedName("recommended_size") public String recommendedSize;
    @SerializedName("fit_preference") public String fitPreference;
    @SerializedName("fit_type") public String fitType;
    @SerializedName("confidence_score") public Double confidenceScore;
    @SerializedName("reason") public String reason;
    @SerializedName("alternatives") public List<RecommendationAlternative> alternatives;
    @SerializedName("created_at") public String createdAt;
    @SerializedName("clothing_item") public ClothingItem clothingItem;
}
