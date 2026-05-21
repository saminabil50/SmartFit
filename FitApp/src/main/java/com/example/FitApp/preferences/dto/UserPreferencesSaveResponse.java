package com.example.FitApp.preferences.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserPreferencesSaveResponse {
    private String message;
    private UserPreferencesResponse preferences;
}
