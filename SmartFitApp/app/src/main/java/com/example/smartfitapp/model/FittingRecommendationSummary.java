package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class FittingRecommendationSummary {
    @SerializedName("recommended_size") public String recommendedSize;
    @SerializedName("fit_preference") public String fitPreference;
    @SerializedName("confidence_score") public Double confidenceScore;
    @SerializedName("reason") public String reason;
}
