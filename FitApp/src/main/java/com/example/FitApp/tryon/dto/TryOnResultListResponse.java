package com.example.FitApp.tryon.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TryOnResultListResponse {
    private List<TryOnResultResponse> items;
}
