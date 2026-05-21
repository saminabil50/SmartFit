package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TryOnResultListResponse {
    @SerializedName("items")
    public List<TryOnResult> items;
}
