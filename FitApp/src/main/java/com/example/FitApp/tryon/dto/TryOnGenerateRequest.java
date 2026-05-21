package com.example.FitApp.tryon.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TryOnGenerateRequest {
    private Long imageId;
    private Long itemId;
    private Long measurementId;
}
