package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ClothingItemRequest {
    @SerializedName("name") public String name;
    @SerializedName("category") public String category;
    @SerializedName("gender") public String gender;
    @SerializedName("available_sizes") public List<String> availableSizes;
    @SerializedName("image_url") public String imageUrl;
}
