package com.example.FitApp.fitting.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FittingMeasurementSummaryResponse {
    private Double chestCm;
    private Double waistCm;
    private Double hipCm;
}
