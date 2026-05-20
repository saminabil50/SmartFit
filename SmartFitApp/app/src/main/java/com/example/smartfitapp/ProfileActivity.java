package com.example.smartfitapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.MessageResponse;
import com.example.smartfitapp.model.UpdateProfileRequest;
import com.example.smartfitapp.model.UpdateProfileResponse;
import com.example.smartfitapp.model.UserResponse;
import com.example.smartfitapp.network.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String[] GENDERS = {"", "male", "female", "other", "prefer_not_to_say"};
    private static final String[] SIZE_SYSTEMS = {"", "US", "UK", "EU", "INT"};

    private EditText fullNameInput, heightInput, weightInput;
    private Spinner genderSpinner, sizeSystemSpinner;
    private Button saveButton, deleteButton;
    private TextView emailText, statusText;
    private ProgressBar progressBar;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authManager = new AuthManager(this);
        if (!authManager.isLoggedIn()) { finish(); return; }

        fullNameInput  = findViewById(R.id.fullNameInput);
        emailText      = findViewById(R.id.emailText);
        heightInput    = findViewById(R.id.heightInput);
        weightInput    = findViewById(R.id.weightInput);
        genderSpinner  = findViewById(R.id.genderSpinner);
        sizeSystemSpinner = findViewById(R.id.sizeSystemSpinner);
        saveButton     = findViewById(R.id.saveButton);
        deleteButton   = findViewById(R.id.deleteButton);
        statusText     = findViewById(R.id.statusText);
        progressBar    = findViewById(R.id.progressBar);

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, GENDERS);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SIZE_SYSTEMS);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSystemSpinner.setAdapter(sizeAdapter);

        saveButton.setOnClickListener(v -> saveProfile());
        deleteButton.setOnClickListener(v -> confirmDeleteAccount());

        loadProfile();
    }

    private void loadProfile() {
        setLoading(true);
        ApiClient.get().getMyProfile(authManager.getBearerToken()).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    populateForm(response.body());
                } else if (response.code() == 401) {
                    authManager.logout();
                    goToLogin();
                }
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                setLoading(false);
                showStatus("Failed to load profile: " + t.getMessage(), true);
            }
        });
    }

    private void populateForm(UserResponse user) {
        fullNameInput.setText(user.fullName != null ? user.fullName : "");
        emailText.setText(user.email != null ? user.email : "");
        if (user.heightCm != null) heightInput.setText(String.valueOf(user.heightCm));
        if (user.weightKg != null) weightInput.setText(String.valueOf(user.weightKg));
        setSpinnerSelection(genderSpinner, GENDERS, user.gender);
        setSpinnerSelection(sizeSystemSpinner, SIZE_SYSTEMS, user.preferredSizeSystem);
    }

    private void setSpinnerSelection(Spinner spinner, String[] options, String value) {
        if (value == null) return;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) { spinner.setSelection(i); return; }
        }
    }

    private void saveProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest();

        String fullName = fullNameInput.getText().toString().trim();
        if (!fullName.isEmpty()) request.fullName = fullName;

        String gender = (String) genderSpinner.getSelectedItem();
        if (gender != null && !gender.isEmpty()) request.gender = gender;

        String heightStr = heightInput.getText().toString().trim();
        if (!heightStr.isEmpty()) {
            try { request.heightCm = Integer.parseInt(heightStr); }
            catch (NumberFormatException e) { showStatus("Invalid height value", true); return; }
        }

        String weightStr = weightInput.getText().toString().trim();
        if (!weightStr.isEmpty()) {
            try { request.weightKg = Double.parseDouble(weightStr); }
            catch (NumberFormatException e) { showStatus("Invalid weight value", true); return; }
        }

        String sizeSystem = (String) sizeSystemSpinner.getSelectedItem();
        if (sizeSystem != null && !sizeSystem.isEmpty()) request.preferredSizeSystem = sizeSystem;

        setLoading(true);
        ApiClient.get().updateProfile(authManager.getBearerToken(), request).enqueue(new Callback<UpdateProfileResponse>() {
            @Override
            public void onResponse(Call<UpdateProfileResponse> call, Response<UpdateProfileResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().profile != null)
                        authManager.saveUser(response.body().profile);
                    showStatus("Profile saved successfully", false);
                } else {
                    showStatus(parseError(response), true);
                }
            }
            @Override
            public void onFailure(Call<UpdateProfileResponse> call, Throwable t) {
                setLoading(false);
                showStatus("Network error: " + t.getMessage(), true);
            }
        });
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        setLoading(true);
        ApiClient.get().deleteAccount(authManager.getBearerToken()).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    authManager.logout();
                    goToLogin();
                } else {
                    showStatus("Failed to delete account", true);
                }
            }
            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                setLoading(false);
                showStatus("Network error: " + t.getMessage(), true);
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!loading);
        deleteButton.setEnabled(!loading);
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
                if (body.contains("\"message\"")) {
                    int start = body.indexOf("\"message\":\"") + 11;
                    int end = body.indexOf("\"", start);
                    if (start > 10 && end > start) return body.substring(start, end);
                }
                return body;
            }
        } catch (Exception ignored) {}
        return "Update failed (HTTP " + response.code() + ")";
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
