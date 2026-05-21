package com.example.FitApp.catalog.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SizeChartResponse {
    private String message;
    private Long itemId;
    private List<String> availableSizes;
    @JsonRawValue
    private String sizeChart;
}
