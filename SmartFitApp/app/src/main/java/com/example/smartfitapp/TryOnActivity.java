package com.example.smartfitapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.ClothingItem;
import com.example.smartfitapp.model.ClothingItemListResponse;
import com.example.smartfitapp.model.FittingResult;
import com.example.smartfitapp.model.FittingResultRequest;
import com.example.smartfitapp.model.TryOnResult;
import com.example.smartfitapp.network.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TryOnActivity extends AppCompatActivity {

    private static final int CATALOG_LIMIT = 100;

    private AuthManager authManager;
    private Button generateButton, cameraButton, galleryButton;
    private ProgressBar progressBar;
    private ImageView selectedImagePreview, resultImage;
    private TextView selectedItemText, resultTitle, warningText, fittingSummaryText;
    private TryOnClothingAdapter clothingAdapter;
    private boolean authErrorShown = false;
    private Uri selectedImageUri;
    private Uri cameraOutputUri;
    private String selectedMimeType = "image/jpeg";
    private ClothingItem selectedClothingItem;

    private final List<ClothingItem> clothingItems = new ArrayList<>();

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
                    loadSelectedPreview();
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
                    loadSelectedPreview();
                }
            }
    );

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

        selectedImagePreview = findViewById(R.id.selectedImagePreview);
        generateButton = findViewById(R.id.generateButton);
        cameraButton = findViewById(R.id.cameraButton);
        galleryButton = findViewById(R.id.galleryButton);
        progressBar = findViewById(R.id.progressBar);
        selectedItemText = findViewById(R.id.selectedItemText);
        resultImage = findViewById(R.id.resultImage);
        resultTitle = findViewById(R.id.resultTitle);
        warningText = findViewById(R.id.warningText);
        fittingSummaryText = findViewById(R.id.fittingSummaryText);

        RecyclerView clothingRecyclerView = findViewById(R.id.clothingRecyclerView);
        clothingAdapter = new TryOnClothingAdapter(clothingItems, this::selectClothingItem);
        clothingRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        clothingRecyclerView.setAdapter(clothingAdapter);

        generateButton.setOnClickListener(v -> generateTryOn());
        cameraButton.setOnClickListener(v -> requestCameraAndLaunch());
        galleryButton.setOnClickListener(v -> launchGallery());
        setLoading(true);
        loadClothingItems();
    }

    private void loadClothingItems() {
        ApiClient.get().getClothingItems(authManager.getBearerToken(), null, null, 0, CATALOG_LIMIT)
                .enqueue(new Callback<ClothingItemListResponse>() {
                    @Override
                    public void onResponse(Call<ClothingItemListResponse> call, Response<ClothingItemListResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                            clothingItems.clear();
                            clothingItems.addAll(response.body().items);
                            clothingAdapter.notifyDataSetChanged();
                        } else if (response.code() == 401) {
                            showAuthError();
                        }
                        updateGenerateButton();
                    }

                    @Override
                    public void onFailure(Call<ClothingItemListResponse> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(TryOnActivity.this, "Failed to load catalog", Toast.LENGTH_SHORT).show();
                        updateGenerateButton();
                    }
                });
    }

    private void selectClothingItem(ClothingItem item) {
        selectedClothingItem = item;
        selectedItemText.setText(item.name != null ? item.name : "Selected item");
        clothingAdapter.setSelectedItemId(item.id);
        updateGenerateButton();
    }

    private void generateTryOn() {
        if (selectedImageUri == null || selectedClothingItem == null) {
            Toast.makeText(this, "Choose a photo and clothing item first", Toast.LENGTH_SHORT).show();
            return;
        }

        ClothingItem item = selectedClothingItem;
        setLoading(true);
        warningText.setVisibility(View.GONE);
        try {
            byte[] bytes = readBytes(selectedImageUri);
            RequestBody reqFile = RequestBody.create(bytes, MediaType.parse(selectedMimeType));
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", "tryon-photo", reqFile);
            RequestBody itemPart = RequestBody.create(String.valueOf(item.id), MediaType.parse("text/plain"));
            ApiClient.get().generateTryOnFromImage(authManager.getBearerToken(), body, itemPart).enqueue(new Callback<TryOnResult>() {
            @Override
            public void onResponse(Call<TryOnResult> call, Response<TryOnResult> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    showResult(response.body());
                    createFittingResultFromTryOn(response.body().imageId, item, response.body());
                } else if (response.code() == 401) {
                    goToLogin();
                } else {
                    showTryOnError("Try-on failed: " + readErrorBody(response));
                }
            }

            @Override
            public void onFailure(Call<TryOnResult> call, Throwable t) {
                setLoading(false);
                Toast.makeText(TryOnActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        } catch (IOException e) {
            setLoading(false);
            Toast.makeText(this, "Failed to read photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String readErrorBody(Response<?> response) {
        if (response.errorBody() == null) return "HTTP " + response.code();
        try {
            String body = response.errorBody().string();
            try {
                JSONObject json = new JSONObject(body);
                if (json.has("message")) return json.getString("message");
                if (json.has("detail")) return json.getString("detail");
                if (json.has("error")) return json.getString("error");
            } catch (Exception ignored) {
                // Fall through to raw response body.
            }
            return body;
        } catch (IOException e) {
            return "HTTP " + response.code();
        }
    }

    private void showTryOnError(String message) {
        warningText.setText(message);
        warningText.setVisibility(View.VISIBLE);
        Toast.makeText(TryOnActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void showResult(TryOnResult result) {
        String name = result.clothingItem != null ? result.clothingItem.name : "Generated preview";
        resultTitle.setText(name);
        resultTitle.setVisibility(View.VISIBLE);
        resultImage.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(ApiClient.fullImageUrl(result.resultImageUrl))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .fitCenter()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        setImageHeight(resultImage, dp(320));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        resizeImageViewToDrawable(resultImage, resource, 320, 220);
                        return false;
                    }
                })
                .into(resultImage);

        if (result.warnings != null && !result.warnings.isEmpty()) {
            warningText.setText(String.join("\n", result.warnings));
            warningText.setVisibility(View.VISIBLE);
        }
    }

    private void resizeImageViewToDrawable(ImageView imageView, Drawable drawable, int fallbackHeightDp, int minHeightDp) {
        imageView.post(() -> {
            int viewWidth = imageView.getWidth();
            int imageWidth = drawable.getIntrinsicWidth();
            int imageHeight = drawable.getIntrinsicHeight();

            if (viewWidth <= 0 || imageWidth <= 0 || imageHeight <= 0) {
                setImageHeight(imageView, dp(fallbackHeightDp));
                return;
            }

            int targetHeight = Math.round(viewWidth * (imageHeight / (float) imageWidth));
            setImageHeight(imageView, Math.max(dp(minHeightDp), targetHeight));
        });
    }

    private void setImageHeight(ImageView imageView, int height) {
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        if (params.height != height) {
            params.height = height;
            imageView.setLayoutParams(params);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void createFittingResultFromTryOn(Long imageId, ClothingItem item, TryOnResult tryOnResult) {
        if (imageId == null) return;
        FittingResultRequest request = new FittingResultRequest(
                imageId,
                item.id,
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

    private void updateGenerateButton() {
        generateButton.setEnabled(selectedImageUri != null && selectedClothingItem != null);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        generateButton.setEnabled(!loading && selectedImageUri != null && selectedClothingItem != null);
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
            File photoFile = File.createTempFile("smartfit_tryon_", ".jpg", getExternalCacheDir());
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

    private void loadSelectedPreview() {
        selectedImagePreview.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(selectedImageUri)
                .fitCenter()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        setImageHeight(selectedImagePreview, dp(260));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        resizeImageViewToDrawable(selectedImagePreview, resource, 260, 220);
                        return false;
                    }
                })
                .into(selectedImagePreview);
        updateGenerateButton();
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
