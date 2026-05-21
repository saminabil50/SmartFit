package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class SizeChartUpdateRequest {
    @SerializedName("size_chart")
    public List<Map<String, Object>> sizeChart;
}
