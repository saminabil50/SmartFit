package com.example.FitApp.user.dto;

import com.example.FitApp.auth.dto.UserResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateProfileResponse {
    private String message;
    private UserResponse profile;
}
