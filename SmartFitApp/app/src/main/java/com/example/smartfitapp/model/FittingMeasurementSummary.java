package com.example.smartfitapp.model;

import com.google.gson.annotations.SerializedName;

public class FittingMeasurementSummary {
    @SerializedName("chest_cm") public Double chestCm;
    @SerializedName("waist_cm") public Double waistCm;
    @SerializedName("hip_cm") public Double hipCm;
}
