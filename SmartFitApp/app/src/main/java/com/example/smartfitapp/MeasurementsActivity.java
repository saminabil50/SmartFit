package com.example.smartfitapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.MeasurementListResponse;
import com.example.smartfitapp.model.MeasurementResponse;
import com.example.smartfitapp.model.MessageResponse;
import com.example.smartfitapp.model.UserResponse;
import com.example.smartfitapp.network.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeasurementsActivity extends AppCompatActivity {

    private AuthManager authManager;
    private MeasurementAdapter adapter;
    private List<MeasurementResponse> measurements = new ArrayList<>();

    private TextInputEditText heightInput;
    private ImageView previewImage;
    private TextView emptyText;
    private RecyclerView recyclerView;
    private Button estimateButton, cameraButton, galleryButton;
    private Uri selectedImageUri;
    private Uri cameraOutputUri;
    private String selectedMimeType = "image/jpeg";

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> { if (granted) launchCamera(); }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && cameraOutputUri != null) {
                    selectedImageUri = cameraOutputUri;
                    selectedMimeType = "image/jpeg";
                    loadPreview();
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    String type = getContentResolver().getType(selectedImageUri);
                    selectedMimeType = type != null ? type : "image/jpeg";
                    loadPreview();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurements);

        authManager = new AuthManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        previewImage = findViewById(R.id.previewImage);
        heightInput = findViewById(R.id.heightInput);
        estimateButton = findViewById(R.id.estimateButton);
        cameraButton = findViewById(R.id.cameraButton);
        galleryButton = findViewById(R.id.galleryButton);
        emptyText = findViewById(R.id.emptyText);
        recyclerView = findViewById(R.id.recyclerView);

        adapter = new MeasurementAdapter(measurements, this::deleteMeasurement);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        estimateButton.setOnClickListener(v -> estimateMeasurements());
        cameraButton.setOnClickListener(v -> requestCameraAndLaunch());
        galleryButton.setOnClickListener(v -> launchGallery());
        estimateButton.setEnabled(false);
        prefillProfileHeight();
        loadMeasurements();
    }

    private void prefillProfileHeight() {
        UserResponse user = authManager.getCurrentUser();
        if (user != null && user.heightCm != null) {
            heightInput.setText(String.valueOf(user.heightCm));
        }
    }

    private void requestCameraAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File photoFile = File.createTempFile("smartfit_measure_", ".jpg", getExternalCacheDir());
            cameraOutputUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(this, "Cannot open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void loadPreview() {
        Glide.with(this).load(selectedImageUri).centerCrop().into(previewImage);
        estimateButton.setEnabled(true);
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
        if (selectedImageUri == null) {
            Toast.makeText(this, "Take or choose a photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        String heightStr = heightInput.getText() != null ? heightInput.getText().toString().trim() : "";
        RequestBody heightPart = null;
        if (!TextUtils.isEmpty(heightStr)) {
            try {
                Integer.parseInt(heightStr);
                heightPart = RequestBody.create(heightStr, MediaType.parse("text/plain"));
            } catch (NumberFormatException e) {
                heightInput.setError("Invalid height");
                return;
            }
        }

        try {
            byte[] bytes = readBytes(selectedImageUri);
            RequestBody reqFile = RequestBody.create(bytes, MediaType.parse(selectedMimeType));
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", "measurement-photo", reqFile);
            estimateButton.setEnabled(false);
            ApiClient.get().estimateMeasurementsFromImage(authManager.getBearerToken(), body, heightPart)
                    .enqueue(new Callback<MeasurementResponse>() {
                        @Override
                        public void onResponse(Call<MeasurementResponse> call, Response<MeasurementResponse> response) {
                            estimateButton.setEnabled(selectedImageUri != null);
                            if (response.isSuccessful() && response.body() != null) {
                                measurements.add(0, response.body());
                                adapter.notifyItemInserted(0);
                                recyclerView.scrollToPosition(0);
                                updateEmptyState();
                                Toast.makeText(MeasurementsActivity.this, "Measurements estimated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MeasurementsActivity.this, "Estimation failed: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<MeasurementResponse> call, Throwable t) {
                            estimateButton.setEnabled(selectedImageUri != null);
                            Toast.makeText(MeasurementsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException e) {
            Toast.makeText(this, "Failed to read photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] readBytes(Uri uri) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IOException("Cannot open URI");
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toByteArray();
        }
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
