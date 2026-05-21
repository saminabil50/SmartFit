package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class RecommendationAlternative {
    @SerializedName("size") public String size;
    @SerializedName("fit_type") public String fitType;
    @SerializedName("confidence_score") public Double confidenceScore;
    @SerializedName("reason") public String reason;
}
