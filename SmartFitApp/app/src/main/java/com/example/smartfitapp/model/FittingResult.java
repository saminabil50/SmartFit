package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FittingResult {
    @SerializedName("id") public Long id;
    @SerializedName("image_id") public Long imageId;
    @SerializedName("item_id") public Long itemId;
    @SerializedName("measurement_id") public Long measurementId;
    @SerializedName("recommendation_id") public Long recommendationId;
    @SerializedName("tryon_id") public Long tryonId;
    @SerializedName("recommended_size") public String recommendedSize;
    @SerializedName("fit_status") public String fitStatus;
    @SerializedName("fit_label") public String fitLabel;
    @SerializedName("confidence_score") public Double confidenceScore;
    @SerializedName("summary") public String summary;
    @SerializedName("warnings") public List<String> warnings;
    @SerializedName("result_image_url") public String resultImageUrl;
    @SerializedName("created_at") public String createdAt;
    @SerializedName("measurement_summary") public FittingMeasurementSummary measurementSummary;
    @SerializedName("recommendation") public FittingRecommendationSummary recommendation;
    @SerializedName("clothing_item") public ClothingItem clothingItem;
}
