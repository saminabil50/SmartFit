package com.example.FitApp.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String gender;
    private Integer heightCm;
    private Double weightKg;
    private String preferredSizeSystem;
}
