package com.example.smartfitapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.LoginRequest;
import com.example.smartfitapp.model.LoginResponse;
import com.example.smartfitapp.network.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView errorText, registerLink;
    private ProgressBar progressBar;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new AuthManager(this);

        emailInput    = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton   = findViewById(R.id.loginButton);
        errorText     = findViewById(R.id.errorText);
        registerLink  = findViewById(R.id.registerLink);
        progressBar   = findViewById(R.id.progressBar);

        loginButton.setOnClickListener(v -> attemptLogin());

        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String email    = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Email and password are required");
            return;
        }

        setLoading(true);
        errorText.setVisibility(View.GONE);

        ApiClient.get().login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    authManager.saveToken(body.accessToken);
                    if (body.user != null) authManager.saveUser(body.user);

                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                // Try to extract "message" from Spring Boot error JSON
                if (body.contains("\"message\"")) {
                    int start = body.indexOf("\"message\":\"") + 11;
                    int end = body.indexOf("\"", start);
                    if (start > 10 && end > start) return body.substring(start, end);
                }
                return body;
            }
        } catch (Exception ignored) {}
        return "Login failed (HTTP " + response.code() + ")";
    }
}
