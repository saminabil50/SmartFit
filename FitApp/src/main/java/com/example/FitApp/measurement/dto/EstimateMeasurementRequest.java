package com.example.FitApp.measurement.dto;

import lombok.Data;

@Data
public class EstimateMeasurementRequest {
    private Long imageId;       // required — image must belong to the authenticated user
    private Integer heightCm;   // optional — overrides user profile height
}
