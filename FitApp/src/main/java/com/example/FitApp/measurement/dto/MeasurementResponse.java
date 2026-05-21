package com.example.FitApp.measurement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeasurementResponse {
    private Long id;
    private Long imageId;
    private Integer heightCmUsed;
    // All body measurements in centimeters
    private Double shoulderWidth;
    private Double chest;
    private Double waist;
    private Double hip;
    private Double inseam;
    private Double confidenceScore;
    private LocalDateTime createdAt;
}
