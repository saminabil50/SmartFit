package com.example.smartfitapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.UserPreferences;
import com.example.smartfitapp.model.UserPreferencesRequest;
import com.example.smartfitapp.model.UserPreferencesSaveResponse;
import com.example.smartfitapp.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreferencesActivity extends AppCompatActivity {

    private static final String[] SIZE_SYSTEMS = {"US", "UK", "EU", "INT"};
    private static final String[] FITS = {"tight", "regular", "loose"};
    private static final String[] GENDER_CATEGORIES = {"male", "female", "unisex"};
    private static final String[] IMAGE_TYPES = {"profile_photo", "mirror_photo", "fitting_photo"};
    private static final String[] CATEGORIES = {
            "tshirt", "shirt", "hoodie", "jacket", "sweater", "pants",
            "jeans", "shorts", "skirt", "dress", "shoes", "accessories"
    };

    private AuthManager authManager;
    private Spinner sizeSystemSpinner, fitSpinner, genderCategorySpinner, imageTypeSpinner;
    private LinearLayout categoriesContainer;
    private Switch cameraSwitch, saveImagesSwitch, saveMeasurementsSwitch, saveTryOnSwitch;
    private CheckBox dataConsentCheckbox;
    private Button saveButton, resetButton;
    private TextView statusText;
    private ProgressBar progressBar;
    private final List<CheckBox> categoryCheckboxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = new AuthManager(this);
        if (!authManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_preferences);

        sizeSystemSpinner = findViewById(R.id.sizeSystemSpinner);
        fitSpinner = findViewById(R.id.fitSpinner);
        genderCategorySpinner = findViewById(R.id.genderCategorySpinner);
        imageTypeSpinner = findViewById(R.id.imageTypeSpinner);
        categoriesContainer = findViewById(R.id.categoriesContainer);
        cameraSwitch = findViewById(R.id.cameraSwitch);
        saveImagesSwitch = findViewById(R.id.saveImagesSwitch);
        saveMeasurementsSwitch = findViewById(R.id.saveMeasurementsSwitch);
        saveTryOnSwitch = findViewById(R.id.saveTryOnSwitch);
        dataConsentCheckbox = findViewById(R.id.dataConsentCheckbox);
        saveButton = findViewById(R.id.saveButton);
        resetButton = findViewById(R.id.resetButton);
        statusText = findViewById(R.id.statusText);
        progressBar = findViewById(R.id.progressBar);

        bindSpinner(sizeSystemSpinner, SIZE_SYSTEMS);
        bindSpinner(fitSpinner, FITS);
        bindSpinner(genderCategorySpinner, GENDER_CATEGORIES);
        bindSpinner(imageTypeSpinner, IMAGE_TYPES);
        bindCategories();

        saveButton.setOnClickListener(v -> savePreferences());
        resetButton.setOnClickListener(v -> resetPreferences());

        loadPreferences();
    }

    private void bindSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void bindCategories() {
        for (String category : CATEGORIES) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(category);
            checkBox.setTextSize(15);
            checkBox.setButtonTintList(null);
            categoriesContainer.addView(checkBox);
            categoryCheckboxes.add(checkBox);
        }
    }

    private void loadPreferences() {
        setLoading(true);
        ApiClient.get().getMyPreferences(authManager.getBearerToken()).enqueue(new Callback<UserPreferences>() {
            @Override
            public void onResponse(Call<UserPreferences> call, Response<UserPreferences> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    populateForm(response.body());
                } else if (response.code() == 401) {
                    authManager.logout();
                    goToLogin();
                } else {
                    showStatus(parseError(response), true);
                }
            }

            @Override
            public void onFailure(Call<UserPreferences> call, Throwable t) {
                setLoading(false);
                showStatus("Failed to load preferences: " + t.getMessage(), true);
            }
        });
    }

    private void populateForm(UserPreferences preferences) {
        setSpinnerSelection(sizeSystemSpinner, SIZE_SYSTEMS, preferences.preferredSizeSystem);
        setSpinnerSelection(fitSpinner, FITS, preferences.preferredFit);
        setSpinnerSelection(genderCategorySpinner, GENDER_CATEGORIES, preferences.preferredGenderCategory);
        setSpinnerSelection(imageTypeSpinner, IMAGE_TYPES, preferences.defaultImageType);
        setSwitch(cameraSwitch, preferences.cameraEnabled, true);
        setSwitch(saveImagesSwitch, preferences.saveUploadedImages, true);
        setSwitch(saveMeasurementsSwitch, preferences.saveMeasurementHistory, true);
        setSwitch(saveTryOnSwitch, preferences.saveTryonHistory, true);
        dataConsentCheckbox.setChecked(Boolean.TRUE.equals(preferences.dataUsageConsent));

        for (CheckBox checkBox : categoryCheckboxes) {
            checkBox.setChecked(preferences.preferredCategories != null
                    && preferences.preferredCategories.contains(checkBox.getText().toString()));
        }
    }

    private void setSwitch(CompoundButton control, Boolean value, boolean fallback) {
        control.setChecked(value != null ? value : fallback);
    }

    private void setSpinnerSelection(Spinner spinner, String[] options, String value) {
        if (value == null) return;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void savePreferences() {
        setLoading(true);
        ApiClient.get().savePreferences(authManager.getBearerToken(), buildRequest())
                .enqueue(new Callback<UserPreferencesSaveResponse>() {
                    @Override
                    public void onResponse(Call<UserPreferencesSaveResponse> call,
                                           Response<UserPreferencesSaveResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().preferences != null) populateForm(response.body().preferences);
                            showStatus(response.body().message != null
                                    ? response.body().message
                                    : "Preferences saved successfully", false);
                        } else if (response.code() == 401) {
                            authManager.logout();
                            goToLogin();
                        } else {
                            showStatus(parseError(response), true);
                        }
                    }

                    @Override
                    public void onFailure(Call<UserPreferencesSaveResponse> call, Throwable t) {
                        setLoading(false);
                        showStatus("Network error: " + t.getMessage(), true);
                    }
                });
    }

    private void resetPreferences() {
        setLoading(true);
        ApiClient.get().resetPreferences(authManager.getBearerToken())
                .enqueue(new Callback<UserPreferencesSaveResponse>() {
                    @Override
                    public void onResponse(Call<UserPreferencesSaveResponse> call,
                                           Response<UserPreferencesSaveResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().preferences != null) populateForm(response.body().preferences);
                            showStatus(response.body().message != null
                                    ? response.body().message
                                    : "Preferences reset to defaults", false);
                        } else if (response.code() == 401) {
                            authManager.logout();
                            goToLogin();
                        } else {
                            showStatus(parseError(response), true);
                        }
                    }

                    @Override
                    public void onFailure(Call<UserPreferencesSaveResponse> call, Throwable t) {
                        setLoading(false);
                        showStatus("Network error: " + t.getMessage(), true);
                    }
                });
    }

    private UserPreferencesRequest buildRequest() {
        UserPreferencesRequest request = new UserPreferencesRequest();
        request.preferredSizeSystem = (String) sizeSystemSpinner.getSelectedItem();
        request.preferredFit = (String) fitSpinner.getSelectedItem();
        request.preferredGenderCategory = (String) genderCategorySpinner.getSelectedItem();
        request.defaultImageType = (String) imageTypeSpinner.getSelectedItem();
        request.cameraEnabled = cameraSwitch.isChecked();
        request.saveUploadedImages = saveImagesSwitch.isChecked();
        request.saveMeasurementHistory = saveMeasurementsSwitch.isChecked();
        request.saveTryonHistory = saveTryOnSwitch.isChecked();
        request.dataUsageConsent = dataConsentCheckbox.isChecked();
        request.preferredCategories = new ArrayList<>();
        for (CheckBox checkBox : categoryCheckboxes) {
            if (checkBox.isChecked()) request.preferredCategories.add(checkBox.getText().toString());
        }
        return request;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!loading);
        resetButton.setEnabled(!loading);
    }

    private void showStatus(String message, boolean isError) {
        statusText.setText(message);
        statusText.setTextColor(isError ? 0xFFB00020 : 0xFF388E3C);
        statusText.setVisibility(View.VISIBLE);
    }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                if (body.contains("\"detail\"")) {
                    int start = body.indexOf("\"detail\":\"") + 10;
                    int end = body.indexOf("\"", start);
                    if (start > 9 && end > start) return body.substring(start, end);
                }
                return body;
            }
        } catch (Exception ignored) {}
        return "Request failed (HTTP " + response.code() + ")";
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
