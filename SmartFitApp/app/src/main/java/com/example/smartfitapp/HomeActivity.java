package com.example.smartfitapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.UserResponse;

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
        Button logoutButton       = findViewById(R.id.logoutButton);

        UserResponse user = authManager.getCurrentUser();
        if (user != null) {
            welcomeText.setText("Welcome, " + user.fullName + "!");
            emailText.setText(user.email);
        }

        profileButton.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        photosButton.setOnClickListener(v ->
                startActivity(new Intent(this, ImageGalleryActivity.class)));
        measurementsButton.setOnClickListener(v ->
                startActivity(new Intent(this, MeasurementsActivity.class)));
        catalogButton.setOnClickListener(v ->
                startActivity(new Intent(this, CatalogActivity.class)));
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
}
