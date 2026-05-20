package com.example.FitApp.image.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImageListResponse {
    private List<ImageResponse> items;
}
