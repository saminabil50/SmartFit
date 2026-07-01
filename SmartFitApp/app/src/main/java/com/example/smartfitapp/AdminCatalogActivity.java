package com.example.smartfitapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.ClothingItem;
import com.example.smartfitapp.model.ClothingItemListResponse;
import com.example.smartfitapp.model.ClothingItemRequest;
import com.example.smartfitapp.model.ClothingItemSaveResponse;
import com.example.smartfitapp.model.MessageResponse;
import com.example.smartfitapp.model.SizeChartUpdateRequest;
import com.example.smartfitapp.model.SizeChartUpdateResponse;
import com.example.smartfitapp.network.ApiClient;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCatalogActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 100;
    private static final String CATEGORY_PLACEHOLDER = "Category";
    private static final String GENDER_PLACEHOLDER = "Gender";
    private static final String[] CATEGORIES = {CATEGORY_PLACEHOLDER, "tshirt", "shirt", "hoodie", "jacket", "sweater", "pants", "jeans", "shorts", "skirt", "dress", "shoes", "accessories"};
    private static final String[] GENDERS = {GENDER_PLACEHOLDER, "male", "female", "unisex"};
    private static final String DEFAULT_SIZE_SYSTEM = "INT";

    private final Gson gson = new Gson();
    private final List<ClothingItem> currentItems = new ArrayList<>();

    private AuthManager authManager;
    private TryOnClothingAdapter currentItemsAdapter;
    private Long selectedItemId;
    private Uri selectedImageUri;
    private String selectedImageMimeType = "image/jpeg";
    private boolean selectedItemHasImage;
    private boolean addMode = true;

    private EditText nameInput, descriptionInput, brandInput;
    private EditText chartSizesInput;
    private TextInputLayout nameInputLayout, descriptionInputLayout, brandInputLayout, chartSizesInputLayout;
    private Spinner categorySpinner, genderSpinner;
    private CheckBox activeCheckbox;
    private LinearLayout currentAvailableSection;
    private MaterialButton addModeButton, updateModeButton, chooseImageButton, saveButton, deactivateButton;
    private TextView statusText, selectedImageText;
    private View selectedImageCard;
    private ImageView selectedImagePreview;
    private ProgressBar progressBar;
    private ScrollView adminCatalogScroll;

    private final ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    String type = getContentResolver().getType(selectedImageUri);
                    selectedImageMimeType = type != null ? type : "image/jpeg";
                    selectedImageText.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
                    selectedImageText.setText("Image selected. It will upload when you save the item.");
                    showSelectedImage(selectedImageUri);
                    showStatus("Image selected", false);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_catalog);

        authManager = new AuthManager(this);
        if (!authManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        nameInput = findViewById(R.id.nameInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        brandInput = findViewById(R.id.brandInput);
        chartSizesInput = findViewById(R.id.chartSizesInput);
        nameInputLayout = findViewById(R.id.nameInputLayout);
        descriptionInputLayout = findViewById(R.id.descriptionInputLayout);
        brandInputLayout = findViewById(R.id.brandInputLayout);
        chartSizesInputLayout = findViewById(R.id.chartSizesInputLayout);
        categorySpinner = findViewById(R.id.categorySpinner);
        genderSpinner = findViewById(R.id.genderSpinner);
        activeCheckbox = findViewById(R.id.activeCheckbox);
        currentAvailableSection = findViewById(R.id.currentAvailableSection);
        addModeButton = findViewById(R.id.addModeButton);
        updateModeButton = findViewById(R.id.updateModeButton);
        chooseImageButton = findViewById(R.id.chooseImageButton);
        saveButton = findViewById(R.id.saveButton);
        deactivateButton = findViewById(R.id.deactivateButton);
        statusText = findViewById(R.id.statusText);
        selectedImageText = findViewById(R.id.selectedImageText);
        selectedImageCard = findViewById(R.id.selectedImageCard);
        selectedImagePreview = findViewById(R.id.selectedImagePreview);
        progressBar = findViewById(R.id.progressBar);
        adminCatalogScroll = findViewById(R.id.adminCatalogScroll);

        bindSpinner(categorySpinner, CATEGORIES);
        bindSpinner(genderSpinner, GENDERS);

        RecyclerView itemsRecyclerView = findViewById(R.id.itemsRecyclerView);
        currentItemsAdapter = new TryOnClothingAdapter(currentItems, item -> loadItem(item.id));
        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        itemsRecyclerView.setAdapter(currentItemsAdapter);

        addModeButton.setOnClickListener(v -> startAddMode());
        updateModeButton.setOnClickListener(v -> startUpdateMode());
        chooseImageButton.setOnClickListener(v -> chooseImage());
        saveButton.setOnClickListener(v -> saveItem());
        deactivateButton.setOnClickListener(v -> confirmDeactivateSelected());

        showInitialMode();
        loadItems();
    }

    private void bindSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadItems() {
        setLoading(true);
        ApiClient.get().getAdminClothingItems(authManager.getBearerToken(), null, null, null, true, null, 0, PAGE_SIZE)
                .enqueue(new Callback<ClothingItemListResponse>() {
                    @Override
                    public void onResponse(Call<ClothingItemListResponse> call, Response<ClothingItemListResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                            currentItems.clear();
                            currentItems.addAll(response.body().items);
                            currentItemsAdapter.notifyDataSetChanged();
                        } else if (response.code() == 401) {
                            authManager.logout();
                            goToLogin();
                        } else if (response.code() == 403) {
                            showStatus("Admin access required", true);
                        } else {
                            showStatus(parseError(response), true);
                        }
                    }

                    @Override
                    public void onFailure(Call<ClothingItemListResponse> call, Throwable t) {
                        setLoading(false);
                        showStatus("Network error: " + t.getMessage(), true);
                    }
                });
    }

    private void loadItem(Long itemId) {
        setLoading(true);
        ApiClient.get().getAdminClothingItem(authManager.getBearerToken(), itemId)
                .enqueue(new Callback<ClothingItem>() {
                    @Override
                    public void onResponse(Call<ClothingItem> call, Response<ClothingItem> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            populateForm(response.body());
                        } else {
                            showStatus(parseError(response), true);
                        }
                    }

                    @Override
                    public void onFailure(Call<ClothingItem> call, Throwable t) {
                        setLoading(false);
                        showStatus("Network error: " + t.getMessage(), true);
                    }
                });
    }

    private void populateForm(ClothingItem item) {
        addMode = false;
        selectedItemId = item.id;
        updateModeButton.setChecked(true);
        selectedImageUri = null;
        clearFieldErrors();
        selectedItemHasImage = item.imageUrl != null && !item.imageUrl.isBlank();
        nameInput.setText(value(item.name));
        descriptionInput.setText(value(item.description));
        brandInput.setText(value(item.brand));
        activeCheckbox.setChecked(!Boolean.FALSE.equals(item.isActive));
        selectedImageText.setText(selectedItemHasImage
                ? "Current image is saved. Choose another image to replace it."
                : "Choose an image before saving.");
        deactivateButton.setVisibility(View.VISIBLE);
        deactivateButton.setEnabled(true);
        if (!selectedItemHasImage) {
            hideSelectedImage();
        } else {
            showSelectedImage(ApiClient.fullImageUrl(item.imageUrl));
        }
        setSpinner(categorySpinner, CATEGORIES, item.category);
        setSpinner(genderSpinner, GENDERS, item.gender);
        populateSizeChart(item);
        currentAvailableSection.setVisibility(View.VISIBLE);
        currentItemsAdapter.setSelectedItemId(item.id);
    }

    private void startAddMode() {
        addMode = true;
        addModeButton.setChecked(true);
        currentAvailableSection.setVisibility(View.GONE);
        clearForm();
        chooseImage();
    }

    private void startUpdateMode() {
        addMode = false;
        updateModeButton.setChecked(true);
        currentAvailableSection.setVisibility(View.VISIBLE);
        selectedImageUri = null;
        clearFieldErrors();
        selectedImageText.setText("Select an item from Current Available to edit it.");
        deactivateButton.setVisibility(selectedItemId == null ? View.GONE : View.VISIBLE);
        currentItemsAdapter.setSelectedItemId(selectedItemId);
    }

    private void clearForm() {
        selectedItemId = null;
        selectedImageUri = null;
        selectedItemHasImage = false;
        nameInput.setText("");
        descriptionInput.setText("");
        brandInput.setText("");
        chartSizesInput.setText("S,M,L,XL");
        activeCheckbox.setChecked(true);
        clearFieldErrors();
        selectedImageText.setText("Choose an image, fill the fields, then Save Item.");
        deactivateButton.setVisibility(View.GONE);
        hideSelectedImage();
        currentItemsAdapter.setSelectedItemId(null);
    }

    private void showInitialMode() {
        addMode = false;
        selectedItemId = null;
        selectedImageUri = null;
        selectedItemHasImage = false;
        updateModeButton.setChecked(true);
        currentAvailableSection.setVisibility(View.GONE);
        clearForm();
        selectedImageText.setText("Choose Add Item to upload a new catalog image, or Update Item to edit an existing item.");
        deactivateButton.setVisibility(View.GONE);
        hideSelectedImage();
    }

    private void saveItem() {
        clearFieldErrors();

        ClothingItemRequest request = buildItemRequest();
        if (request == null) return;

        List<Map<String, Object>> sizeChart = buildSizeChartRows();
        if (sizeChart == null) return;

        if (selectedItemId == null && selectedImageUri == null) {
            showImageRequiredError();
            return;
        }
        if (selectedItemId != null && selectedImageUri == null && !selectedItemHasImage) {
            showImageRequiredError();
            return;
        }

        setLoading(true);
        if (selectedItemId == null) {
            ApiClient.get().createAdminClothingItem(authManager.getBearerToken(), request)
                    .enqueue(new Callback<ClothingItem>() {
                        @Override
                        public void onResponse(Call<ClothingItem> call, Response<ClothingItem> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                selectedItemId = response.body().id;
                                saveSizeChartThenImage(sizeChart, "Clothing item created successfully");
                            } else {
                                setLoading(false);
                                showStatus(parseError(response), true);
                            }
                        }

                        @Override
                        public void onFailure(Call<ClothingItem> call, Throwable t) {
                            setLoading(false);
                            showStatus("Network error: " + t.getMessage(), true);
                        }
                    });
        } else {
            ApiClient.get().updateAdminClothingItem(authManager.getBearerToken(), selectedItemId, request)
                    .enqueue(new Callback<ClothingItemSaveResponse>() {
                        @Override
                        public void onResponse(Call<ClothingItemSaveResponse> call, Response<ClothingItemSaveResponse> response) {
                            if (response.isSuccessful()) {
                                saveSizeChartThenImage(sizeChart, "Clothing item saved successfully");
                            } else {
                                setLoading(false);
                                showStatus(parseError(response), true);
                            }
                        }

                        @Override
                        public void onFailure(Call<ClothingItemSaveResponse> call, Throwable t) {
                            setLoading(false);
                            showStatus("Network error: " + t.getMessage(), true);
                        }
                    });
        }
    }

    private ClothingItemRequest buildItemRequest() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            showFieldRequiredError(nameInputLayout, nameInput, "Name is required");
            return null;
        }
        String description = descriptionInput.getText().toString().trim();
        if (description.isEmpty()) {
            showFieldRequiredError(descriptionInputLayout, descriptionInput, "Description is required");
            return null;
        }
        String brand = brandInput.getText().toString().trim();
        if (brand.isEmpty()) {
            showFieldRequiredError(brandInputLayout, brandInput, "Brand is required");
            return null;
        }
        String category = (String) categorySpinner.getSelectedItem();
        if (CATEGORY_PLACEHOLDER.equals(category)) {
            showSpinnerRequiredError(categorySpinner, "Category is required");
            return null;
        }
        String gender = (String) genderSpinner.getSelectedItem();
        if (GENDER_PLACEHOLDER.equals(gender)) {
            showSpinnerRequiredError(genderSpinner, "Gender is required");
            return null;
        }

        ClothingItemRequest request = new ClothingItemRequest();
        request.name = name;
        request.description = description;
        request.category = category;
        request.gender = gender;
        request.brand = brand;
        request.sizeSystem = DEFAULT_SIZE_SYSTEM;
        request.availableSizes = extractSizeLabels();
        request.isActive = activeCheckbox.isChecked();
        return request;
    }

    private List<Map<String, Object>> buildSizeChartRows() {
        List<String> sizes = extractSizeLabels();
        if (sizes.isEmpty()) {
            showFieldRequiredError(chartSizesInputLayout, chartSizesInput, "At least one size is required");
            return null;
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String size : sizes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("size", size);
            rows.add(row);
        }
        return rows;
    }

    private void saveSizeChartThenImage(List<Map<String, Object>> sizeChart, String successMessage) {
        SizeChartUpdateRequest request = new SizeChartUpdateRequest();
        request.sizeChart = sizeChart;
        ApiClient.get().updateAdminSizeChart(authManager.getBearerToken(), selectedItemId, request)
                .enqueue(new Callback<SizeChartUpdateResponse>() {
                    @Override
                    public void onResponse(Call<SizeChartUpdateResponse> call, Response<SizeChartUpdateResponse> response) {
                        if (response.isSuccessful()) {
                            uploadSelectedImageThenFinish(successMessage);
                        } else {
                            setLoading(false);
                            showStatus(parseError(response), true);
                        }
                    }

                    @Override
                    public void onFailure(Call<SizeChartUpdateResponse> call, Throwable t) {
                        setLoading(false);
                        showStatus("Network error: " + t.getMessage(), true);
                    }
                });
    }

    private void uploadSelectedImageThenFinish(String successMessage) {
        if (selectedImageUri == null) {
            finishSave(successMessage);
            return;
        }
        try {
            byte[] bytes = readBytes(selectedImageUri);
            RequestBody reqFile = RequestBody.create(bytes, MediaType.parse(selectedImageMimeType));
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", "catalog-image", reqFile);
            ApiClient.get().uploadAdminClothingItemImage(authManager.getBearerToken(), selectedItemId, body)
                    .enqueue(new Callback<ClothingItemSaveResponse>() {
                        @Override
                        public void onResponse(Call<ClothingItemSaveResponse> call, Response<ClothingItemSaveResponse> response) {
                            if (response.isSuccessful()) {
                                selectedImageUri = null;
                                selectedItemHasImage = true;
                                finishSave(successMessage);
                            } else {
                                setLoading(false);
                                showStatus(parseError(response), true);
                            }
                        }

                        @Override
                        public void onFailure(Call<ClothingItemSaveResponse> call, Throwable t) {
                            setLoading(false);
                            showStatus("Network error: " + t.getMessage(), true);
                        }
                    });
        } catch (IOException e) {
            setLoading(false);
            showStatus("Failed to read image: " + e.getMessage(), true);
        }
    }

    private void finishSave(String successMessage) {
        setLoading(false);
        showStatus(successMessage, false);
        loadItems();
        if (selectedItemId != null) loadItem(selectedItemId);
    }

    private void confirmDeactivateSelected() {
        if (selectedItemId == null) {
            showStatus("Select an item to deactivate", true);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Deactivate Item")
                .setMessage("Deactivate this catalog item? It will no longer appear to clients.")
                .setPositiveButton("Deactivate", (dialog, which) -> deactivateSelectedItem())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deactivateSelectedItem() {
        Long itemId = selectedItemId;
        setLoading(true);
        ApiClient.get().deactivateAdminClothingItem(authManager.getBearerToken(), itemId)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            clearForm();
                            startUpdateMode();
                            loadItems();
                            showStatus("Clothing item deactivated successfully", false);
                        } else {
                            showStatus(parseError(response), true);
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        setLoading(false);
                        showStatus("Network error: " + t.getMessage(), true);
                    }
                });
    }

    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePicker.launch(intent);
    }

    private void showSelectedImage(Object imageSource) {
        selectedImageCard.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(imageSource)
                .fitCenter()
                .into(selectedImagePreview);
    }

    private void hideSelectedImage() {
        selectedImageCard.setVisibility(View.GONE);
        selectedImagePreview.setImageDrawable(null);
    }

    private void populateSizeChart(ClothingItem item) {
        List<String> sizes = item.availableSizes != null ? item.availableSizes : List.of();
        chartSizesInput.setText(String.join(",", sizes));
    }

    private List<String> extractSizeLabels() {
        List<String> sizes = new ArrayList<>();
        for (String size : chartSizesInput.getText().toString().split(",")) {
            String trimmed = size.trim();
            if (!trimmed.isEmpty()) sizes.add(trimmed);
        }
        return sizes;
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
        saveButton.setEnabled(!loading);
        deactivateButton.setEnabled(!loading && selectedItemId != null);
        addModeButton.setEnabled(!loading);
        updateModeButton.setEnabled(!loading);
        chooseImageButton.setEnabled(!loading);
    }

    private void showStatus(String message, boolean isError) {
        statusText.setText(message == null ? "" : message);
        statusText.setTextColor(ContextCompat.getColor(this, isError ? R.color.error : R.color.success));
        statusText.setVisibility(View.VISIBLE);
    }

    private void showFieldRequiredError(TextInputLayout layout, EditText input, String message) {
        layout.setError(message);
        input.requestFocus();
        scrollToView(layout);
    }

    private void showSpinnerRequiredError(Spinner spinner, String message) {
        spinner.requestFocus();
        scrollToView(spinner);
    }

    private void showImageRequiredError() {
        selectedImageText.setText("Image is required");
        selectedImageText.setTextColor(ContextCompat.getColor(this, R.color.error));
        chooseImageButton.requestFocus();
        scrollToView(chooseImageButton);
    }

    private void clearFieldErrors() {
        nameInputLayout.setError(null);
        descriptionInputLayout.setError(null);
        brandInputLayout.setError(null);
        chartSizesInputLayout.setError(null);
        selectedImageText.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
    }

    private void scrollToView(View view) {
        adminCatalogScroll.post(() -> adminCatalogScroll.smoothScrollTo(0, view.getTop()));
    }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                if (body.contains("\"detail\"")) {
                    int start = body.indexOf("\"detail\":\"") + 10;
                    int end = body.indexOf("\"", start);
                    if (start > 9 && end > start) return body.substring(start, end);
                }
                return body;
            }
        } catch (Exception ignored) {}
        return "Request failed (HTTP " + response.code() + ")";
    }

    private void setSpinner(Spinner spinner, String[] options, String value) {
        if (value == null) return;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
