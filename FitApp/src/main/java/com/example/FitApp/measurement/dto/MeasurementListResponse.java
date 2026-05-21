package com.example.FitApp.measurement.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MeasurementListResponse {
    private List<MeasurementResponse> items;
}
