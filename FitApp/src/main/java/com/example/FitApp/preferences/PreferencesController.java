package com.example.FitApp.preferences;

import com.example.FitApp.preferences.dto.UserPreferencesRequest;
import com.example.FitApp.preferences.dto.UserPreferencesResponse;
import com.example.FitApp.preferences.dto.UserPreferencesSaveResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
public class PreferencesController {

    private final PreferencesService preferencesService;

    @GetMapping("/me")
    public UserPreferencesResponse getMyPreferences(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return preferencesService.getMyPreferences(user);
    }

    @PutMapping("/me")
    public UserPreferencesSaveResponse savePreferences(Authentication authentication,
                                                       @RequestBody(required = false) UserPreferencesRequest request) {
        User user = (User) authentication.getPrincipal();
        return preferencesService.replace(user, request);
    }

    @PatchMapping("/me")
    public UserPreferencesSaveResponse updatePreferences(Authentication authentication,
                                                         @RequestBody(required = false) UserPreferencesRequest request) {
        User user = (User) authentication.getPrincipal();
        return preferencesService.patch(user, request);
    }

    @DeleteMapping("/me")
    public UserPreferencesSaveResponse resetPreferences(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return preferencesService.reset(user);
    }
}
