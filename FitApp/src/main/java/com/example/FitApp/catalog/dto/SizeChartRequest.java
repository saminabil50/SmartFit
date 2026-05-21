package com.example.FitApp.catalog.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SizeChartRequest {
    private List<Map<String, Object>> sizeChart;
}
