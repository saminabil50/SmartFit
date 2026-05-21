package com.example.smartfitapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.ImageItem;
import com.example.smartfitapp.model.UserPreferences;
import com.example.smartfitapp.network.ApiClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageUploadActivity extends AppCompatActivity {

    private static final String[] IMAGE_TYPES = {"fitting_photo", "profile_photo", "mirror_photo"};

    private ImageView previewImage;
    private Spinner imageTypeSpinner;
    private Button cameraButton, galleryButton, uploadButton;
    private TextView statusText;
    private ProgressBar progressBar;

    private AuthManager authManager;
    private Uri selectedImageUri;
    private Uri cameraOutputUri;
    private String selectedMimeType = "image/jpeg";
    private boolean cameraAllowed = true;

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
                    loadPreview(selectedImageUri);
                    uploadButton.setEnabled(true);
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    String type = getContentResolver().getType(selectedImageUri);
                    selectedMimeType = (type != null) ? type : "image/jpeg";
                    loadPreview(selectedImageUri);
                    uploadButton.setEnabled(true);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_upload);

        authManager = new AuthManager(this);
        if (!authManager.isLoggedIn()) { finish(); return; }

        previewImage     = findViewById(R.id.previewImage);
        imageTypeSpinner = findViewById(R.id.imageTypeSpinner);
        cameraButton     = findViewById(R.id.cameraButton);
        galleryButton    = findViewById(R.id.galleryButton);
        uploadButton     = findViewById(R.id.uploadButton);
        statusText       = findViewById(R.id.statusText);
        progressBar      = findViewById(R.id.progressBar);

        uploadButton.setEnabled(false);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, IMAGE_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        imageTypeSpinner.setAdapter(adapter);

        cameraButton.setOnClickListener(v -> requestCameraAndLaunch());
        galleryButton.setOnClickListener(v -> launchGallery());
        uploadButton.setOnClickListener(v -> uploadImage());

        loadPreferences();
    }

    private void loadPreferences() {
        ApiClient.get().getMyPreferences(authManager.getBearerToken()).enqueue(new Callback<UserPreferences>() {
            @Override
            public void onResponse(Call<UserPreferences> call, Response<UserPreferences> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserPreferences preferences = response.body();
                    setSpinnerSelection(preferences.defaultImageType);
                    if (Boolean.FALSE.equals(preferences.cameraEnabled)) {
                        cameraAllowed = false;
                        cameraButton.setEnabled(false);
                        cameraButton.setText("Camera Disabled");
                    }
                } else if (response.code() == 401) {
                    authManager.logout();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<UserPreferences> call, Throwable t) {
                showStatus("Using default upload settings", false);
            }
        });
    }

    private void setSpinnerSelection(String imageType) {
        if (imageType == null) return;
        for (int i = 0; i < IMAGE_TYPES.length; i++) {
            if (IMAGE_TYPES[i].equals(imageType)) {
                imageTypeSpinner.setSelection(i);
                return;
            }
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
            File photoFile = File.createTempFile("smartfit_", ".jpg", getExternalCacheDir());
            cameraOutputUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            showStatus("Cannot open camera: " + e.getMessage(), true);
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void loadPreview(Uri uri) {
        Glide.with(this).load(uri).centerCrop().into(previewImage);
    }

    private void uploadImage() {
        if (selectedImageUri == null) return;

        setLoading(true);
        statusText.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                byte[] bytes = readBytes(selectedImageUri);
                String imageType = (String) imageTypeSpinner.getSelectedItem();

                RequestBody reqFile = RequestBody.create(bytes, MediaType.parse(selectedMimeType));
                MultipartBody.Part body = MultipartBody.Part.createFormData("image", "photo.jpg", reqFile);
                RequestBody typePart = RequestBody.create(imageType, MediaType.parse("text/plain"));

                runOnUiThread(() ->
                    ApiClient.get().uploadImage(authManager.getBearerToken(), body, typePart)
                            .enqueue(new Callback<ImageItem>() {
                                @Override
                                public void onResponse(Call<ImageItem> call, Response<ImageItem> response) {
                                    setLoading(false);
                                    if (response.isSuccessful()) {
                                        showStatus("Upload successful!", false);
                                        uploadButton.setEnabled(false);
                                        selectedImageUri = null;
                                    } else {
                                        showStatus(parseError(response), true);
                                    }
                                }
                                @Override
                                public void onFailure(Call<ImageItem> call, Throwable t) {
                                    setLoading(false);
                                    showStatus("Upload failed: " + t.getMessage(), true);
                                }
                            })
                );
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showStatus("Failed to read image: " + e.getMessage(), true);
                });
            }
        }).start();
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

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        uploadButton.setEnabled(!loading && selectedImageUri != null);
        cameraButton.setEnabled(!loading && cameraAllowed);
        galleryButton.setEnabled(!loading);
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
                if (body.contains("\"detail\"")) {
                    int start = body.indexOf("\"detail\":\"") + 10;
                    int end = body.indexOf("\"", start);
                    if (start > 9 && end > start) return body.substring(start, end);
                }
                return body;
            }
        } catch (Exception ignored) {}
        return "Upload failed (HTTP " + response.code() + ")";
    }
}
