package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ClothingItemRequest {
    @SerializedName("name") public String name;
    @SerializedName("description") public String description;
    @SerializedName("category") public String category;
    @SerializedName("gender") public String gender;
    @SerializedName("brand") public String brand;
    @SerializedName("size_system") public String sizeSystem;
    @SerializedName("available_sizes") public List<String> availableSizes;
    @SerializedName("base_price") public Double basePrice;
    @SerializedName("currency") public String currency;
    @SerializedName("image_url") public String imageUrl;
    @SerializedName("is_active") public Boolean isActive;
}
