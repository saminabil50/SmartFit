package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ClothingItemListResponse {
    @SerializedName("items") public List<ClothingItem> items;
    @SerializedName("total") public long total;
    @SerializedName("page") public int page;
    @SerializedName("limit") public int limit;
}
