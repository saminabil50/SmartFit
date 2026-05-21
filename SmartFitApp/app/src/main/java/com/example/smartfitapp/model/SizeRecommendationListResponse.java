package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SizeRecommendationListResponse {
    @SerializedName("items")
    public List<SizeRecommendation> items;
}
