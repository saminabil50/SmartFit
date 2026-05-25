package com.example.smartfitapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
        ImageButton profileButton = findViewById(R.id.profileButton);
        Button photosButton       = findViewById(R.id.photosButton);
        Button catalogButton      = findViewById(R.id.catalogButton);
        Button tryOnButton        = findViewById(R.id.tryOnButton);
        Button adminCatalogButton = findViewById(R.id.adminCatalogButton);

        UserResponse user = authManager.getCurrentUser();
        if (user != null) {
            welcomeText.setText("Welcome, " + user.fullName + "!");
            emailText.setText(user.email);
            adminCatalogButton.setVisibility("admin".equals(user.role) ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        refreshCurrentUser(adminCatalogButton);

        profileButton.setOnClickListener(this::showProfileMenu);
        photosButton.setOnClickListener(v ->
                startActivity(new Intent(this, ImageGalleryActivity.class)));
        catalogButton.setOnClickListener(v ->
                startActivity(new Intent(this, CatalogActivity.class)));
        tryOnButton.setOnClickListener(v ->
                startActivity(new Intent(this, TryOnActivity.class)));
        adminCatalogButton.setOnClickListener(v ->
                startActivity(new Intent(this, AdminCatalogActivity.class)));
    }

    private void showProfileMenu(View anchor) {
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setBackgroundColor(Color.WHITE);
        int width = dp(180);

        PopupWindow popupWindow = new PopupWindow(
                menuLayout,
                width,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(dp(8));
        }

        TextView profileOption = createProfileMenuItem("My Profile");
        TextView logoutOption = createProfileMenuItem("Logout");
        menuLayout.addView(profileOption);
        menuLayout.addView(logoutOption);

        profileOption.setOnClickListener(v -> {
            popupWindow.dismiss();
            startActivity(new Intent(this, ProfileActivity.class));
        });
        logoutOption.setOnClickListener(v -> {
            popupWindow.dismiss();
            authManager.logout();
            goToLogin();
        });

        popupWindow.showAsDropDown(anchor, -width + anchor.getWidth(), dp(6));
    }

    private TextView createProfileMenuItem(String text) {
        TextView item = new TextView(this);
        item.setText(text);
        item.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        item.setTextSize(15);
        item.setGravity(android.view.Gravity.CENTER_VERTICAL);
        item.setPadding(dp(16), 0, dp(16), 0);
        item.setBackgroundColor(Color.WHITE);
        item.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        ));
        item.setOnHoverListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                v.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant));
            } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                v.setBackgroundColor(Color.WHITE);
            }
            return false;
        });
        item.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_container));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.setBackgroundColor(Color.WHITE);
            }
            return false;
        });
        return item;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
