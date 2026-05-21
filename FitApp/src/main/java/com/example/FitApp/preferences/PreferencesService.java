package com.example.FitApp.preferences;

import com.example.FitApp.preferences.dto.UserPreferencesRequest;
import com.example.FitApp.preferences.dto.UserPreferencesResponse;
import com.example.FitApp.preferences.dto.UserPreferencesSaveResponse;
import com.example.FitApp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PreferencesService {

    public static final String DEFAULT_SIZE_SYSTEM = "INT";
    public static final String DEFAULT_FIT = "regular";
    public static final String DEFAULT_GENDER_CATEGORY = "unisex";
    public static final String DEFAULT_IMAGE_TYPE = "fitting_photo";

    private static final Set<String> ALLOWED_SIZE_SYSTEMS = Set.of("US", "UK", "EU", "INT");
    private static final Set<String> ALLOWED_FITS = Set.of("tight", "regular", "loose");
    private static final Set<String> ALLOWED_GENDER_CATEGORIES = Set.of("male", "female", "unisex");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("profile_photo", "mirror_photo", "fitting_photo");
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "tshirt", "shirt", "hoodie", "jacket", "sweater", "pants",
            "jeans", "shorts", "skirt", "dress", "shoes", "accessories"
    );

    private final UserPreferencesRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserPreferencesResponse getMyPreferences(User user) {
        return toResponse(getOrCreate(user));
    }

    @Transactional
    public UserPreferencesSaveResponse replace(User user, UserPreferencesRequest request) {
        UserPreferences preferences = repository.findByUserId(user.getId())
                .orElseGet(() -> defaults(user));
        applyDefaults(preferences);
        if (request != null) {
            preferences.setPreferredSizeSystem(normalizeSizeSystem(request.getPreferredSizeSystem(), DEFAULT_SIZE_SYSTEM));
            preferences.setPreferredFit(normalizeFit(request.getPreferredFit(), DEFAULT_FIT));
            preferences.setPreferredGenderCategory(normalizeGenderCategory(
                    request.getPreferredGenderCategory(), DEFAULT_GENDER_CATEGORY));
            preferences.setPreferredCategories(serializeCategories(validateCategories(
                    request.getPreferredCategories() == null ? Collections.emptyList() : request.getPreferredCategories())));
            preferences.setDefaultImageType(normalizeImageType(request.getDefaultImageType(), DEFAULT_IMAGE_TYPE));
            preferences.setCameraEnabled(request.getCameraEnabled() != null ? request.getCameraEnabled() : true);
            preferences.setSaveUploadedImages(request.getSaveUploadedImages() != null ? request.getSaveUploadedImages() : true);
            preferences.setSaveMeasurementHistory(request.getSaveMeasurementHistory() != null ? request.getSaveMeasurementHistory() : true);
            preferences.setSaveTryonHistory(request.getSaveTryonHistory() != null ? request.getSaveTryonHistory() : true);
            preferences.setDataUsageConsent(request.getDataUsageConsent() != null ? request.getDataUsageConsent() : false);
        }
        UserPreferences saved = repository.save(preferences);
        return UserPreferencesSaveResponse.builder()
                .message("Preferences saved successfully")
                .preferences(toResponse(saved))
                .build();
    }

    @Transactional
    public UserPreferencesSaveResponse patch(User user, UserPreferencesRequest request) {
        UserPreferences preferences = getOrCreate(user);
        if (request != null) {
            if (request.getPreferredSizeSystem() != null)
                preferences.setPreferredSizeSystem(normalizeSizeSystem(request.getPreferredSizeSystem(), DEFAULT_SIZE_SYSTEM));
            if (request.getPreferredFit() != null)
                preferences.setPreferredFit(normalizeFit(request.getPreferredFit(), DEFAULT_FIT));
            if (request.getPreferredGenderCategory() != null)
                preferences.setPreferredGenderCategory(normalizeGenderCategory(
                        request.getPreferredGenderCategory(), DEFAULT_GENDER_CATEGORY));
            if (request.getPreferredCategories() != null)
                preferences.setPreferredCategories(serializeCategories(validateCategories(request.getPreferredCategories())));
            if (request.getDefaultImageType() != null)
                preferences.setDefaultImageType(normalizeImageType(request.getDefaultImageType(), DEFAULT_IMAGE_TYPE));
            if (request.getCameraEnabled() != null) preferences.setCameraEnabled(request.getCameraEnabled());
            if (request.getSaveUploadedImages() != null) preferences.setSaveUploadedImages(request.getSaveUploadedImages());
            if (request.getSaveMeasurementHistory() != null) preferences.setSaveMeasurementHistory(request.getSaveMeasurementHistory());
            if (request.getSaveTryonHistory() != null) preferences.setSaveTryonHistory(request.getSaveTryonHistory());
            if (request.getDataUsageConsent() != null) preferences.setDataUsageConsent(request.getDataUsageConsent());
        }
        UserPreferences saved = repository.save(preferences);
        return UserPreferencesSaveResponse.builder()
                .message("Preferences updated successfully")
                .preferences(toResponse(saved))
                .build();
    }

    @Transactional
    public UserPreferencesSaveResponse reset(User user) {
        UserPreferences preferences = repository.findByUserId(user.getId())
                .orElseGet(() -> defaults(user));
        applyDefaults(preferences);
        UserPreferences saved = repository.save(preferences);
        return UserPreferencesSaveResponse.builder()
                .message("Preferences reset to defaults")
                .preferences(toResponse(saved))
                .build();
    }

    @Transactional
    public void deleteForUser(User user) {
        repository.deleteByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public String preferredFitOrDefault(User user) {
        return repository.findByUserId(user.getId())
                .map(UserPreferences::getPreferredFit)
                .filter(value -> !value.isBlank())
                .orElse(DEFAULT_FIT);
    }

    @Transactional(readOnly = true)
    public String defaultImageTypeOrDefault(User user) {
        return repository.findByUserId(user.getId())
                .map(UserPreferences::getDefaultImageType)
                .filter(value -> !value.isBlank())
                .orElse(DEFAULT_IMAGE_TYPE);
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponse findExisting(User user) {
        return repository.findByUserId(user.getId()).map(this::toResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean shouldSaveMeasurementHistory(User user) {
        return repository.findByUserId(user.getId())
                .map(UserPreferences::getSaveMeasurementHistory)
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public boolean shouldSaveTryonHistory(User user) {
        return repository.findByUserId(user.getId())
                .map(UserPreferences::getSaveTryonHistory)
                .orElse(true);
    }

    private UserPreferences getOrCreate(User user) {
        return repository.findByUserId(user.getId())
                .orElseGet(() -> repository.save(defaults(user)));
    }

    private UserPreferences defaults(User user) {
        UserPreferences preferences = UserPreferences.builder().userId(user.getId()).build();
        applyDefaults(preferences);
        return preferences;
    }

    private void applyDefaults(UserPreferences preferences) {
        preferences.setPreferredSizeSystem(DEFAULT_SIZE_SYSTEM);
        preferences.setPreferredFit(DEFAULT_FIT);
        preferences.setPreferredGenderCategory(DEFAULT_GENDER_CATEGORY);
        preferences.setPreferredCategories("[]");
        preferences.setDefaultImageType(DEFAULT_IMAGE_TYPE);
        preferences.setCameraEnabled(true);
        preferences.setSaveUploadedImages(true);
        preferences.setSaveMeasurementHistory(true);
        preferences.setSaveTryonHistory(true);
        preferences.setDataUsageConsent(false);
    }

    private String normalizeSizeSystem(String value, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim().toUpperCase();
        if (!ALLOWED_SIZE_SYSTEMS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid preferred_size_system. Allowed values: US, UK, EU, INT");
        }
        return normalized;
    }

    private String normalizeFit(String value, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim().toLowerCase();
        if (!ALLOWED_FITS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid preferred_fit. Allowed values: tight, regular, loose");
        }
        return normalized;
    }

    private String normalizeGenderCategory(String value, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim().toLowerCase();
        if (!ALLOWED_GENDER_CATEGORIES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid preferred_gender_category. Allowed values: male, female, unisex");
        }
        return normalized;
    }

    private String normalizeImageType(String value, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim().toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid default_image_type");
        }
        return normalized;
    }

    private List<String> validateCategories(List<String> categories) {
        if (categories == null) return Collections.emptyList();
        List<String> normalized = categories.stream()
                .map(category -> category == null ? "" : category.trim().toLowerCase())
                .filter(category -> !category.isBlank())
                .distinct()
                .toList();
        if (normalized.stream().anyMatch(category -> !ALLOWED_CATEGORIES.contains(category))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid preferred_categories value");
        }
        return normalized;
    }

    private String serializeCategories(List<String> categories) {
        try {
            return objectMapper.writeValueAsString(categories);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save preferences");
        }
    }

    private List<String> deserializeCategories(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JacksonException e) {
            return Collections.emptyList();
        }
    }

    private UserPreferencesResponse toResponse(UserPreferences preferences) {
        return UserPreferencesResponse.builder()
                .id(preferences.getId())
                .userId(preferences.getUserId())
                .preferredSizeSystem(preferences.getPreferredSizeSystem())
                .preferredFit(preferences.getPreferredFit())
                .preferredGenderCategory(preferences.getPreferredGenderCategory())
                .preferredCategories(deserializeCategories(preferences.getPreferredCategories()))
                .defaultImageType(preferences.getDefaultImageType())
                .cameraEnabled(preferences.getCameraEnabled())
                .saveUploadedImages(preferences.getSaveUploadedImages())
                .saveMeasurementHistory(preferences.getSaveMeasurementHistory())
                .saveTryonHistory(preferences.getSaveTryonHistory())
                .dataUsageConsent(preferences.getDataUsageConsent())
                .createdAt(preferences.getCreatedAt())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }
}
