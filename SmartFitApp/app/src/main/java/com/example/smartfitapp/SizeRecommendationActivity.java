package com.example.smartfitapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.*;
import com.example.smartfitapp.network.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SizeRecommendationActivity extends AppCompatActivity implements SizeRecommendationAdapter.OnDeleteListener {

    private static final int CATALOG_LIMIT = 100;
    private static final String[] FIT_PREFERENCES = {"regular", "tight", "loose"};

    private AuthManager authManager;
    private Spinner measurementSpinner, itemSpinner, fitPreferenceSpinner;
    private Button recommendButton;
    private ProgressBar progressBar;
    private TextView resultSizeText, confidenceText, reasonText, alternativesText, fittingSummaryText, emptyHistoryText;
    private SizeRecommendationAdapter adapter;

    private final List<MeasurementResponse> measurements = new ArrayList<>();
    private final List<ClothingItem> clothingItems = new ArrayList<>();
    private final List<SizeRecommendation> recommendations = new ArrayList<>();
    private boolean authErrorShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_size_recommendation);

        authManager = new AuthManager(this);
        if (!authManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        measurementSpinner = findViewById(R.id.measurementSpinner);
        itemSpinner = findViewById(R.id.itemSpinner);
        fitPreferenceSpinner = findViewById(R.id.fitPreferenceSpinner);
        recommendButton = findViewById(R.id.recommendButton);
        progressBar = findViewById(R.id.progressBar);
        resultSizeText = findViewById(R.id.resultSizeText);
        confidenceText = findViewById(R.id.confidenceText);
        reasonText = findViewById(R.id.reasonText);
        alternativesText = findViewById(R.id.alternativesText);
        fittingSummaryText = findViewById(R.id.fittingSummaryText);
        emptyHistoryText = findViewById(R.id.emptyHistoryText);

        fitPreferenceSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, FIT_PREFERENCES));

        RecyclerView recyclerView = findViewById(R.id.historyRecyclerView);
        adapter = new SizeRecommendationAdapter(recommendations, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        recommendButton.setOnClickListener(v -> recommendSize());
        setLoading(true);
        loadMeasurements();
        loadClothingItems();
        loadHistory();
    }

    private void loadMeasurements() {
        ApiClient.get().getMyMeasurements(authManager.getBearerToken())
                .enqueue(new Callback<MeasurementListResponse>() {
                    @Override
                    public void onResponse(Call<MeasurementListResponse> call, Response<MeasurementListResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                            measurements.clear();
                            measurements.addAll(response.body().items);
                            refreshMeasurementSpinner();
                        } else if (response.code() == 401) {
                            showAuthError();
                        }
                        updateRecommendButton();
                    }

                    @Override
                    public void onFailure(Call<MeasurementListResponse> call, Throwable t) {
                        Toast.makeText(SizeRecommendationActivity.this, "Failed to load measurements", Toast.LENGTH_SHORT).show();
                        updateRecommendButton();
                    }
                });
    }

    private void loadClothingItems() {
        ApiClient.get().getClothingItems(authManager.getBearerToken(), null, null, 0, CATALOG_LIMIT)
                .enqueue(new Callback<ClothingItemListResponse>() {
                    @Override
                    public void onResponse(Call<ClothingItemListResponse> call, Response<ClothingItemListResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                            clothingItems.clear();
                            clothingItems.addAll(response.body().items);
                            refreshItemSpinner();
                        } else if (response.code() == 401) {
                            showAuthError();
                        }
                        updateRecommendButton();
                    }

                    @Override
                    public void onFailure(Call<ClothingItemListResponse> call, Throwable t) {
                        Toast.makeText(SizeRecommendationActivity.this, "Failed to load catalog", Toast.LENGTH_SHORT).show();
                        updateRecommendButton();
                    }
                });
    }

    private void loadHistory() {
        ApiClient.get().getRecommendations(authManager.getBearerToken())
                .enqueue(new Callback<SizeRecommendationListResponse>() {
                    @Override
                    public void onResponse(Call<SizeRecommendationListResponse> call, Response<SizeRecommendationListResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                            adapter.setItems(response.body().items);
                            updateHistoryEmptyState();
                        } else if (response.code() == 401) {
                            showAuthError();
                        }
                    }

                    @Override
                    public void onFailure(Call<SizeRecommendationListResponse> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(SizeRecommendationActivity.this, "Failed to load recommendation history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void refreshMeasurementSpinner() {
        List<String> labels = new ArrayList<>();
        for (MeasurementResponse measurement : measurements) {
            String date = measurement.createdAt != null && measurement.createdAt.length() >= 10
                    ? measurement.createdAt.substring(0, 10)
                    : "measurement";
            labels.add("#" + measurement.id + " - " + date);
        }
        measurementSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private void refreshItemSpinner() {
        List<String> labels = new ArrayList<>();
        for (ClothingItem item : clothingItems) {
            labels.add("#" + item.id + " - " + item.name);
        }
        itemSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private void recommendSize() {
        if (measurements.isEmpty() || clothingItems.isEmpty()) {
            Toast.makeText(this, "Select a measurement and clothing item first", Toast.LENGTH_SHORT).show();
            return;
        }

        MeasurementResponse measurement = measurements.get(measurementSpinner.getSelectedItemPosition());
        ClothingItem item = clothingItems.get(itemSpinner.getSelectedItemPosition());
        String fitPreference = FIT_PREFERENCES[fitPreferenceSpinner.getSelectedItemPosition()];

        setLoading(true);
        SizeRecommendationRequest request = new SizeRecommendationRequest(measurement.id, item.id, fitPreference);
        ApiClient.get().recommendSize(authManager.getBearerToken(), request)
                .enqueue(new Callback<SizeRecommendation>() {
                    @Override
                    public void onResponse(Call<SizeRecommendation> call, Response<SizeRecommendation> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            showResult(response.body());
                            adapter.addFirst(response.body());
                            updateHistoryEmptyState();
                            createFittingResultFromRecommendation(measurement, item, response.body());
                        } else if (response.code() == 401) {
                            goToLogin();
                        } else {
                            Toast.makeText(SizeRecommendationActivity.this, "Recommendation failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SizeRecommendation> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(SizeRecommendationActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showResult(SizeRecommendation result) {
        resultSizeText.setText("Recommended size: " + safe(result.recommendedSize));
        confidenceText.setText("Confidence: " + formatPercent(result.confidenceScore));
        reasonText.setText(result.reason != null ? result.reason : "");

        if (result.alternatives != null && !result.alternatives.isEmpty()) {
            List<String> lines = new ArrayList<>();
            for (RecommendationAlternative alternative : result.alternatives) {
                lines.add(alternative.size + " (" + alternative.fitType + ", " + formatPercent(alternative.confidenceScore) + ")");
            }
            alternativesText.setText("Alternatives: " + String.join("  |  ", lines));
            alternativesText.setVisibility(View.VISIBLE);
        } else {
            alternativesText.setVisibility(View.GONE);
        }

        resultSizeText.setVisibility(View.VISIBLE);
        confidenceText.setVisibility(View.VISIBLE);
        reasonText.setVisibility(View.VISIBLE);
    }

    private void createFittingResultFromRecommendation(MeasurementResponse measurement, ClothingItem item,
                                                       SizeRecommendation recommendation) {
        if (measurement.imageId == null) {
            fittingSummaryText.setText("Size recommendation saved. Upload-linked measurement is needed to create a fitting result.");
            fittingSummaryText.setVisibility(View.VISIBLE);
            return;
        }

        FittingResultRequest request = new FittingResultRequest(
                measurement.imageId,
                item.id,
                measurement.id,
                recommendation.id,
                null
        );
        ApiClient.get().createFittingResult(authManager.getBearerToken(), request)
                .enqueue(new Callback<FittingResult>() {
                    @Override
                    public void onResponse(Call<FittingResult> call, Response<FittingResult> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            showFittingSummary(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<FittingResult> call, Throwable t) {
                        fittingSummaryText.setText("Size recommendation saved. Fitting result could not be created.");
                        fittingSummaryText.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showFittingSummary(FittingResult result) {
        String size = result.recommendedSize != null ? "Size " + result.recommendedSize + " - " : "";
        String label = result.fitLabel != null ? result.fitLabel : "Fitting Result";
        String summary = result.summary != null ? result.summary : "";
        fittingSummaryText.setText("Fitting result saved: " + size + label + "\n" + summary);
        fittingSummaryText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDelete(SizeRecommendation recommendation, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recommendation")
                .setMessage("Delete this size recommendation?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecommendation(recommendation, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecommendation(SizeRecommendation recommendation, int position) {
        ApiClient.get().deleteRecommendation(authManager.getBearerToken(), recommendation.id)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            adapter.removeItem(position);
                            updateHistoryEmptyState();
                        } else if (response.code() == 401) {
                            goToLogin();
                        } else {
                            Toast.makeText(SizeRecommendationActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        Toast.makeText(SizeRecommendationActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateRecommendButton() {
        recommendButton.setEnabled(!measurements.isEmpty() && !clothingItems.isEmpty());
    }

    private void updateHistoryEmptyState() {
        emptyHistoryText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recommendButton.setEnabled(!loading && !measurements.isEmpty() && !clothingItems.isEmpty());
    }

    private void showAuthError() {
        if (authErrorShown) return;
        authErrorShown = true;
        setLoading(false);
        reasonText.setText("Authentication failed while loading recommendations. Please log in again if this continues.");
        reasonText.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Authentication failed while loading recommendations", Toast.LENGTH_LONG).show();
    }

    private void goToLogin() {
        authManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String formatPercent(Double value) {
        return String.format("%.0f%%", (value != null ? value : 0.0) * 100.0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
