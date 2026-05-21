package com.example.smartfitapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.UserResponse;
import com.example.smartfitapp.network.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = new AuthManager(this);

        if (!authManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_home);

        TextView welcomeText = findViewById(R.id.welcomeText);
        TextView emailText   = findViewById(R.id.emailText);
        Button profileButton      = findViewById(R.id.profileButton);
        Button photosButton       = findViewById(R.id.photosButton);
        Button measurementsButton = findViewById(R.id.measurementsButton);
        Button catalogButton      = findViewById(R.id.catalogButton);
        Button tryOnButton        = findViewById(R.id.tryOnButton);
        Button sizeRecommendationButton = findViewById(R.id.sizeRecommendationButton);
        Button preferencesButton  = findViewById(R.id.preferencesButton);
        Button adminCatalogButton = findViewById(R.id.adminCatalogButton);
        Button logoutButton       = findViewById(R.id.logoutButton);

        UserResponse user = authManager.getCurrentUser();
        if (user != null) {
            welcomeText.setText("Welcome, " + user.fullName + "!");
            emailText.setText(user.email);
            adminCatalogButton.setVisibility("admin".equals(user.role) ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        refreshCurrentUser(adminCatalogButton);

        profileButton.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        photosButton.setOnClickListener(v ->
                startActivity(new Intent(this, ImageGalleryActivity.class)));
        measurementsButton.setOnClickListener(v ->
                startActivity(new Intent(this, MeasurementsActivity.class)));
        catalogButton.setOnClickListener(v ->
                startActivity(new Intent(this, CatalogActivity.class)));
        tryOnButton.setOnClickListener(v ->
                startActivity(new Intent(this, TryOnActivity.class)));
        sizeRecommendationButton.setOnClickListener(v ->
                startActivity(new Intent(this, SizeRecommendationActivity.class)));
        preferencesButton.setOnClickListener(v ->
                startActivity(new Intent(this, PreferencesActivity.class)));
        adminCatalogButton.setOnClickListener(v ->
                startActivity(new Intent(this, AdminCatalogActivity.class)));
        logoutButton.setOnClickListener(v -> {
            authManager.logout();
            goToLogin();
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void refreshCurrentUser(Button adminCatalogButton) {
        ApiClient.get().getMe(authManager.getBearerToken()).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    authManager.saveUser(response.body());
                    adminCatalogButton.setVisibility("admin".equals(response.body().role)
                            ? android.view.View.VISIBLE
                            : android.view.View.GONE);
                } else if (response.code() == 401) {
                    authManager.logout();
                    goToLogin();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                // Existing cached user remains usable for navigation.
            }
        });
    }
}
