package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SizeChartUpdateResponse {
    public String message;
    @SerializedName("item_id")
    public Long itemId;
    @SerializedName("available_sizes")
    public List<String> availableSizes;
    @SerializedName("size_chart")
    public Object sizeChart;
}
