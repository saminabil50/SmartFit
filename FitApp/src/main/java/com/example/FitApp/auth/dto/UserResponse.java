package com.example.FitApp.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    // Profile fields (null when returned from auth endpoints)
    private String gender;
    private Integer heightCm;
    private Double weightKg;
    private String preferredSizeSystem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
