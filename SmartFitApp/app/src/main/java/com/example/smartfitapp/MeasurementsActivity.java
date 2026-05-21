package com.example.smartfitapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.EstimateMeasurementRequest;
import com.example.smartfitapp.model.MeasurementListResponse;
import com.example.smartfitapp.model.MeasurementResponse;
import com.example.smartfitapp.model.MessageResponse;
import com.example.smartfitapp.network.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeasurementsActivity extends AppCompatActivity {

    private AuthManager authManager;
    private MeasurementAdapter adapter;
    private List<MeasurementResponse> measurements = new ArrayList<>();

    private TextInputEditText imageIdInput, heightInput;
    private TextView emptyText;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurements);

        authManager = new AuthManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageIdInput = findViewById(R.id.imageIdInput);
        heightInput = findViewById(R.id.heightInput);
        Button estimateButton = findViewById(R.id.estimateButton);
        emptyText = findViewById(R.id.emptyText);
        recyclerView = findViewById(R.id.recyclerView);

        adapter = new MeasurementAdapter(measurements, this::deleteMeasurement);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        estimateButton.setOnClickListener(v -> estimateMeasurements());
        loadMeasurements();
    }

    private void loadMeasurements() {
        ApiClient.get().getMyMeasurements(authManager.getBearerToken())
                .enqueue(new Callback<MeasurementListResponse>() {
                    @Override
                    public void onResponse(Call<MeasurementListResponse> call, Response<MeasurementListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            measurements.clear();
                            if (response.body().items != null) {
                                measurements.addAll(response.body().items);
                            }
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        }
                    }

                    @Override
                    public void onFailure(Call<MeasurementListResponse> call, Throwable t) {
                        Toast.makeText(MeasurementsActivity.this, "Failed to load measurements", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void estimateMeasurements() {
        String imageIdStr = imageIdInput.getText() != null ? imageIdInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(imageIdStr)) {
            imageIdInput.setError("Image ID is required");
            return;
        }

        long imageId;
        try {
            imageId = Long.parseLong(imageIdStr);
        } catch (NumberFormatException e) {
            imageIdInput.setError("Invalid Image ID");
            return;
        }

        String heightStr = heightInput.getText() != null ? heightInput.getText().toString().trim() : "";
        Integer heightCm = null;
        if (!TextUtils.isEmpty(heightStr)) {
            try {
                heightCm = Integer.parseInt(heightStr);
            } catch (NumberFormatException e) {
                heightInput.setError("Invalid height");
                return;
            }
        }

        EstimateMeasurementRequest request = new EstimateMeasurementRequest(imageId, heightCm);
        ApiClient.get().estimateMeasurements(authManager.getBearerToken(), request)
                .enqueue(new Callback<MeasurementResponse>() {
                    @Override
                    public void onResponse(Call<MeasurementResponse> call, Response<MeasurementResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            measurements.add(0, response.body());
                            adapter.notifyItemInserted(0);
                            recyclerView.scrollToPosition(0);
                            updateEmptyState();
                            imageIdInput.setText("");
                            heightInput.setText("");
                            Toast.makeText(MeasurementsActivity.this, "Measurements estimated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MeasurementsActivity.this, "Estimation failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MeasurementResponse> call, Throwable t) {
                        Toast.makeText(MeasurementsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteMeasurement(MeasurementResponse measurement) {
        ApiClient.get().deleteMeasurement(authManager.getBearerToken(), measurement.id)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            int idx = measurements.indexOf(measurement);
                            if (idx >= 0) {
                                measurements.remove(idx);
                                adapter.notifyItemRemoved(idx);
                                updateEmptyState();
                            }
                        } else {
                            Toast.makeText(MeasurementsActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        Toast.makeText(MeasurementsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyState() {
        emptyText.setVisibility(measurements.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
