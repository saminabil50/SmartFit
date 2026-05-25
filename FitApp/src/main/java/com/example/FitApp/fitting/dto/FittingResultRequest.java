package com.example.FitApp.fitting.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FittingResultRequest {
    private Long imageId;
    private Long itemId;
    private Long tryonId;
}
