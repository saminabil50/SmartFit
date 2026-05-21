package com.example.smartfitapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.ClothingItem;
import com.example.smartfitapp.model.ClothingItemListResponse;
import com.example.smartfitapp.model.FittingResult;
import com.example.smartfitapp.model.FittingResultRequest;
import com.example.smartfitapp.model.ImageItem;
import com.example.smartfitapp.model.ImageListResponse;
import com.example.smartfitapp.model.MeasurementListResponse;
import com.example.smartfitapp.model.MeasurementResponse;
import com.example.smartfitapp.model.MessageResponse;
import com.example.smartfitapp.model.TryOnGenerateRequest;
import com.example.smartfitapp.model.TryOnResult;
import com.example.smartfitapp.model.TryOnResultListResponse;
import com.example.smartfitapp.network.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TryOnActivity extends AppCompatActivity implements TryOnResultAdapter.OnDeleteListener {

    private static final int CATALOG_LIMIT = 100;

    private AuthManager authManager;
    private Spinner imageSpinner, itemSpinner, measurementSpinner;
    private Button generateButton;
    private ProgressBar progressBar;
    private ImageView resultImage;
    private TextView resultTitle, warningText, fittingSummaryText, emptyHistoryText;
    private TryOnResultAdapter adapter;
    private boolean authErrorShown = false;

    private final List<ImageItem> images = new ArrayList<>();
    private final List<ClothingItem> clothingItems = new ArrayList<>();
    private final List<MeasurementResponse> measurements = new ArrayList<>();
    private final List<TryOnResult> tryOnResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tryon);

        authManager = new AuthManager(this);
        if (!authManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageSpinner = findViewById(R.id.imageSpinner);
        itemSpinner = findViewById(R.id.itemSpinner);
        measurementSpinner = findViewById(R.id.measurementSpinner);
        generateButton = findViewById(R.id.generateButton);
        progressBar = findViewById(R.id.progressBar);
        resultImage = findViewById(R.id.resultImage);
        resultTitle = findViewById(R.id.resultTitle);
        warningText = findViewById(R.id.warningText);
        fittingSummaryText = findViewById(R.id.fittingSummaryText);
        emptyHistoryText = findViewById(R.id.emptyHistoryText);

        RecyclerView recyclerView = findViewById(R.id.historyRecyclerView);
        adapter = new TryOnResultAdapter(tryOnResults, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        generateButton.setOnClickListener(v -> generateTryOn());
        setLoading(true);
        loadImages();
        loadClothingItems();
        loadMeasurements();
        loadHistory();
    }

    private void loadImages() {
        ApiClient.get().getMyImages(authManager.getBearerToken()).enqueue(new Callback<ImageListResponse>() {
            @Override
            public void onResponse(Call<ImageListResponse> call, Response<ImageListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                    images.clear();
                    images.addAll(response.body().items);
                    refreshImageSpinner();
                } else if (response.code() == 401) {
                    showAuthError();
                }
                updateGenerateButton();
            }

            @Override
            public void onFailure(Call<ImageListResponse> call, Throwable t) {
                Toast.makeText(TryOnActivity.this, "Failed to load images", Toast.LENGTH_SHORT).show();
                updateGenerateButton();
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
                        updateGenerateButton();
                    }

                    @Override
                    public void onFailure(Call<ClothingItemListResponse> call, Throwable t) {
                        Toast.makeText(TryOnActivity.this, "Failed to load catalog", Toast.LENGTH_SHORT).show();
                        updateGenerateButton();
                    }
                });
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
                    }

                    @Override
                    public void onFailure(Call<MeasurementListResponse> call, Throwable t) {
                        refreshMeasurementSpinner();
                    }
                });
    }

    private void loadHistory() {
        ApiClient.get().getTryOnResults(authManager.getBearerToken()).enqueue(new Callback<TryOnResultListResponse>() {
            @Override
            public void onResponse(Call<TryOnResultListResponse> call, Response<TryOnResultListResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                    adapter.setItems(response.body().items);
                    updateHistoryEmptyState();
                } else if (response.code() == 401) {
                    showAuthError();
                }
            }

            @Override
            public void onFailure(Call<TryOnResultListResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(TryOnActivity.this, "Failed to load try-on history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshImageSpinner() {
        List<String> labels = new ArrayList<>();
        for (ImageItem image : images) {
            String type = image.imageType != null ? image.imageType.replace("_", " ") : "image";
            labels.add("#" + image.id + " - " + type);
        }
        imageSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private void refreshItemSpinner() {
        List<String> labels = new ArrayList<>();
        for (ClothingItem item : clothingItems) {
            labels.add("#" + item.id + " - " + item.name);
        }
        itemSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private void refreshMeasurementSpinner() {
        List<String> labels = new ArrayList<>();
        labels.add("No measurement");
        for (MeasurementResponse measurement : measurements) {
            String date = measurement.createdAt != null && measurement.createdAt.length() >= 10
                    ? measurement.createdAt.substring(0, 10)
                    : "measurement";
            labels.add("#" + measurement.id + " - " + date);
        }
        measurementSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private void generateTryOn() {
        if (images.isEmpty() || clothingItems.isEmpty()) {
            Toast.makeText(this, "Select an uploaded image and clothing item first", Toast.LENGTH_SHORT).show();
            return;
        }

        ImageItem image = images.get(imageSpinner.getSelectedItemPosition());
        ClothingItem item = clothingItems.get(itemSpinner.getSelectedItemPosition());
        Long selectedMeasurementId = null;
        int measurementPosition = measurementSpinner.getSelectedItemPosition();
        if (measurementPosition > 0 && measurementPosition - 1 < measurements.size()) {
            selectedMeasurementId = measurements.get(measurementPosition - 1).id;
        }
        final Long measurementId = selectedMeasurementId;

        setLoading(true);
        warningText.setVisibility(View.GONE);
        TryOnGenerateRequest request = new TryOnGenerateRequest(image.id, item.id, measurementId);
        ApiClient.get().generateTryOn(authManager.getBearerToken(), request).enqueue(new Callback<TryOnResult>() {
            @Override
            public void onResponse(Call<TryOnResult> call, Response<TryOnResult> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    showResult(response.body());
                    adapter.addFirst(response.body());
                    updateHistoryEmptyState();
                    createFittingResultFromTryOn(image, item, measurementId, response.body());
                } else if (response.code() == 401) {
                    goToLogin();
                } else {
                    Toast.makeText(TryOnActivity.this, "Try-on failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TryOnResult> call, Throwable t) {
                setLoading(false);
                Toast.makeText(TryOnActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResult(TryOnResult result) {
        String name = result.clothingItem != null ? result.clothingItem.name : "Generated preview";
        resultTitle.setText(name);
        resultTitle.setVisibility(View.VISIBLE);
        resultImage.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(ApiClient.fullImageUrl(result.resultImageUrl))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(resultImage);

        if (result.warnings != null && !result.warnings.isEmpty()) {
            warningText.setText(String.join("\n", result.warnings));
            warningText.setVisibility(View.VISIBLE);
        }
    }

    private void createFittingResultFromTryOn(ImageItem image, ClothingItem item, Long measurementId, TryOnResult tryOnResult) {
        FittingResultRequest request = new FittingResultRequest(
                image.id,
                item.id,
                measurementId,
                null,
                tryOnResult.id
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
                        fittingSummaryText.setText("Try-on saved. Fitting result could not be created.");
                        fittingSummaryText.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showFittingSummary(FittingResult result) {
        String label = result.fitLabel != null ? result.fitLabel : "Fitting Result";
        String summary = result.summary != null ? result.summary : "";
        fittingSummaryText.setText("Fitting result saved: " + label + "\n" + summary);
        fittingSummaryText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDelete(TryOnResult result, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Try-On")
                .setMessage("Delete this try-on result?")
                .setPositiveButton("Delete", (dialog, which) -> deleteResult(result, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteResult(TryOnResult result, int position) {
        ApiClient.get().deleteTryOnResult(authManager.getBearerToken(), result.id)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            adapter.removeItem(position);
                            updateHistoryEmptyState();
                        } else if (response.code() == 401) {
                            goToLogin();
                        } else {
                            Toast.makeText(TryOnActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        Toast.makeText(TryOnActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateGenerateButton() {
        generateButton.setEnabled(!images.isEmpty() && !clothingItems.isEmpty());
    }

    private void updateHistoryEmptyState() {
        emptyHistoryText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        generateButton.setEnabled(!loading && !images.isEmpty() && !clothingItems.isEmpty());
    }

    private void showAuthError() {
        if (authErrorShown) return;
        authErrorShown = true;
        setLoading(false);
        warningText.setText("Authentication failed while loading try-on data. Please log in again if this continues.");
        warningText.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Authentication failed while loading try-on data", Toast.LENGTH_LONG).show();
    }

    private void goToLogin() {
        authManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
