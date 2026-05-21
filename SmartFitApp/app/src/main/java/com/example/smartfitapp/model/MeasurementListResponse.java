package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MeasurementListResponse {
    @SerializedName("items") public List<MeasurementResponse> items;
}
