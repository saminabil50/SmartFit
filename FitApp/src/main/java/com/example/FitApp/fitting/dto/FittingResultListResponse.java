package com.example.FitApp.fitting.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FittingResultListResponse {
    private List<FittingResultResponse> items;
}
