package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FittingResultListResponse {
    @SerializedName("items")
    public List<FittingResult> items;
}
