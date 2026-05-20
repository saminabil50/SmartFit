package com.example.smartfitapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartfitapp.model.RegisterRequest;
import com.example.smartfitapp.model.UserResponse;
import com.example.smartfitapp.network.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText fullNameInput, emailInput, passwordInput, confirmPasswordInput;
    private Button registerButton;
    private TextView errorText, loginLink;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        fullNameInput        = findViewById(R.id.fullNameInput);
        emailInput           = findViewById(R.id.emailInput);
        passwordInput        = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton       = findViewById(R.id.registerButton);
        errorText            = findViewById(R.id.errorText);
        loginLink            = findViewById(R.id.loginLink);
        progressBar          = findViewById(R.id.progressBar);

        registerButton.setOnClickListener(v -> attemptRegister());

        loginLink.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String fullName        = fullNameInput.getText().toString().trim();
        String email           = emailInput.getText().toString().trim();
        String password        = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();





        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("All fields are required");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        setLoading(true);
        errorText.setVisibility(View.GONE);

        ApiClient.get().register(new RegisterRequest(fullName, email, password)).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    // Registration successful — go to login
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("registered_email", email);
                    startActivity(intent);
                    finish();
                } else {
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                setLoading(false);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!loading);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
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
        return "Registration failed (HTTP " + response.code() + ")";
    }
}
