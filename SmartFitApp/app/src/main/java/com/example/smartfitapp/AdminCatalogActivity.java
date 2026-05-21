package com.example.smartfitapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.ClothingItem;
import com.example.smartfitapp.model.ClothingItemListResponse;
import com.example.smartfitapp.model.ClothingItemRequest;
import com.example.smartfitapp.model.ClothingItemSaveResponse;
import com.example.smartfitapp.model.MessageResponse;
import com.example.smartfitapp.model.SizeChartUpdateRequest;
import com.example.smartfitapp.model.SizeChartUpdateResponse;
import com.example.smartfitapp.network.ApiClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCatalogActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 20;
    private static final String[] CATEGORIES = {"tshirt", "shirt", "hoodie", "jacket", "sweater", "pants", "jeans", "shorts", "skirt", "dress", "shoes", "accessories"};
    private static final String[] GENDERS = {"male", "female", "unisex"};
    private static final String[] SIZE_SYSTEMS = {"US", "UK", "EU", "INT"};

    private final Gson gson = new Gson();
    private AuthManager authManager;
    private Long selectedItemId;
    private Uri selectedImageUri;
    private String selectedImageMimeType = "image/jpeg";

    private LinearLayout itemsContainer;
    private EditText searchInput, nameInput, descriptionInput, brandInput, sizesInput, priceInput, currencyInput, imageUrlInput, sizeChartInput;
    private Spinner categorySpinner, genderSpinner, sizeSystemSpinner;
    private CheckBox activeCheckbox;
    private Button searchButton, newButton, saveButton, deactivateButton, chooseImageButton, uploadImageButton, saveSizeChartButton;
    private TextView statusText;
    private ProgressBar progressBar;

    private final ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    String type = getContentResolver().getType(selectedImageUri);
                    selectedImageMimeType = type != null ? type : "image/jpeg";
                    uploadImageButton.setEnabled(selectedItemId != null);
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

        itemsContainer = findViewById(R.id.itemsContainer);
        searchInput = findViewById(R.id.searchInput);
        nameInput = findViewById(R.id.nameInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        brandInput = findViewById(R.id.brandInput);
        sizesInput = findViewById(R.id.sizesInput);
        priceInput = findViewById(R.id.priceInput);
        currencyInput = findViewById(R.id.currencyInput);
        imageUrlInput = findViewById(R.id.imageUrlInput);
        sizeChartInput = findViewById(R.id.sizeChartInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        genderSpinner = findViewById(R.id.genderSpinner);
        sizeSystemSpinner = findViewById(R.id.sizeSystemSpinner);
        activeCheckbox = findViewById(R.id.activeCheckbox);
        searchButton = findViewById(R.id.searchButton);
        newButton = findViewById(R.id.newButton);
        saveButton = findViewById(R.id.saveButton);
        deactivateButton = findViewById(R.id.deactivateButton);
        chooseImageButton = findViewById(R.id.chooseImageButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        saveSizeChartButton = findViewById(R.id.saveSizeChartButton);
        statusText = findViewById(R.id.statusText);
        progressBar = findViewById(R.id.progressBar);

        bindSpinner(categorySpinner, CATEGORIES);
        bindSpinner(genderSpinner, GENDERS);
        bindSpinner(sizeSystemSpinner, SIZE_SYSTEMS);

        searchButton.setOnClickListener(v -> loadItems());
        newButton.setOnClickListener(v -> clearForm());
        saveButton.setOnClickListener(v -> saveItem());
        deactivateButton.setOnClickListener(v -> deactivateItem());
        chooseImageButton.setOnClickListener(v -> chooseImage());
        uploadImageButton.setOnClickListener(v -> uploadImage());
        saveSizeChartButton.setOnClickListener(v -> saveSizeChart());

        clearForm();
        loadItems();
    }

    private void bindSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadItems() {
        setLoading(true);
        String search = searchInput.getText().toString().trim();
        ApiClient.get().getAdminClothingItems(authManager.getBearerToken(), null, null, null, null,
                        search.isEmpty() ? null : search, 0, PAGE_SIZE)
                .enqueue(new Callback<ClothingItemListResponse>() {
                    @Override
                    public void onResponse(Call<ClothingItemListResponse> call, Response<ClothingItemListResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            renderItems(response.body().items);
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

    private void renderItems(List<ClothingItem> items) {
        itemsContainer.removeAllViews();
        if (items == null || items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No catalog items found");
            itemsContainer.addView(empty);
            return;
        }
        for (ClothingItem item : items) {
            Button button = new Button(this);
            String status = Boolean.FALSE.equals(item.isActive) ? "inactive" : "active";
            button.setText(item.id + " - " + item.name + " (" + status + ")");
            button.setAllCaps(false);
            button.setOnClickListener(v -> loadItem(item.id));
            itemsContainer.addView(button);
        }
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
        selectedItemId = item.id;
        nameInput.setText(value(item.name));
        descriptionInput.setText(value(item.description));
        brandInput.setText(value(item.brand));
        sizesInput.setText(item.availableSizes == null ? "" : String.join(",", item.availableSizes));
        priceInput.setText(item.basePrice == null ? "" : String.valueOf(item.basePrice));
        currencyInput.setText(value(item.currency));
        imageUrlInput.setText(value(item.imageUrl));
        sizeChartInput.setText(item.sizeChart == null ? "" : gson.toJson(item.sizeChart));
        activeCheckbox.setChecked(!Boolean.FALSE.equals(item.isActive));
        setSpinner(categorySpinner, CATEGORIES, item.category);
        setSpinner(genderSpinner, GENDERS, item.gender);
        setSpinner(sizeSystemSpinner, SIZE_SYSTEMS, item.sizeSystem);
        deactivateButton.setEnabled(true);
        chooseImageButton.setEnabled(true);
        uploadImageButton.setEnabled(selectedImageUri != null);
        saveSizeChartButton.setEnabled(true);
        showStatus("Editing item " + item.id, false);
    }

    private void clearForm() {
        selectedItemId = null;
        selectedImageUri = null;
        nameInput.setText("");
        descriptionInput.setText("");
        brandInput.setText("");
        sizesInput.setText("");
        priceInput.setText("");
        currencyInput.setText("JOD");
        imageUrlInput.setText("");
        sizeChartInput.setText("[{\"size\":\"M\",\"chest_cm_min\":94,\"chest_cm_max\":102,\"waist_cm_min\":80,\"waist_cm_max\":88}]");
        activeCheckbox.setChecked(true);
        setSpinner(sizeSystemSpinner, SIZE_SYSTEMS, "INT");
        deactivateButton.setEnabled(false);
        chooseImageButton.setEnabled(false);
        uploadImageButton.setEnabled(false);
        saveSizeChartButton.setEnabled(false);
        showStatus("Creating new item", false);
    }

    private void saveItem() {
        ClothingItemRequest request = buildItemRequest();
        if (request == null) return;
        setLoading(true);
        if (selectedItemId == null) {
            ApiClient.get().createAdminClothingItem(authManager.getBearerToken(), request)
                    .enqueue(new Callback<ClothingItem>() {
                        @Override
                        public void onResponse(Call<ClothingItem> call, Response<ClothingItem> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null) {
                                populateForm(response.body());
                                loadItems();
                                showStatus("Clothing item created successfully", false);
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
        } else {
            ApiClient.get().updateAdminClothingItem(authManager.getBearerToken(), selectedItemId, request)
                    .enqueue(new Callback<ClothingItemSaveResponse>() {
                        @Override
                        public void onResponse(Call<ClothingItemSaveResponse> call, Response<ClothingItemSaveResponse> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null) {
                                if (response.body().item != null) populateForm(response.body().item);
                                loadItems();
                                showStatus(response.body().message, false);
                            } else {
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
            showStatus("Name is required", true);
            return null;
        }
        List<String> sizes = new ArrayList<>();
        for (String size : sizesInput.getText().toString().split(",")) {
            if (!size.trim().isEmpty()) sizes.add(size.trim());
        }
        if (sizes.isEmpty()) {
            showStatus("Available sizes are required", true);
            return null;
        }

        ClothingItemRequest request = new ClothingItemRequest();
        request.name = name;
        request.description = textOrNull(descriptionInput);
        request.category = (String) categorySpinner.getSelectedItem();
        request.gender = (String) genderSpinner.getSelectedItem();
        request.brand = textOrNull(brandInput);
        request.sizeSystem = (String) sizeSystemSpinner.getSelectedItem();
        request.availableSizes = sizes;
        request.currency = textOrNull(currencyInput);
        request.imageUrl = textOrNull(imageUrlInput);
        request.isActive = activeCheckbox.isChecked();

        String price = priceInput.getText().toString().trim();
        if (!price.isEmpty()) {
            try {
                request.basePrice = Double.parseDouble(price);
            } catch (NumberFormatException e) {
                showStatus("Invalid price", true);
                return null;
            }
        }
        return request;
    }

    private void deactivateItem() {
        if (selectedItemId == null) return;
        setLoading(true);
        ApiClient.get().deactivateAdminClothingItem(authManager.getBearerToken(), selectedItemId)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            activeCheckbox.setChecked(false);
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

    private void uploadImage() {
        if (selectedItemId == null || selectedImageUri == null) return;
        setLoading(true);
        try {
            byte[] bytes = readBytes(selectedImageUri);
            RequestBody reqFile = RequestBody.create(bytes, MediaType.parse(selectedImageMimeType));
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", "catalog-image", reqFile);
            ApiClient.get().uploadAdminClothingItemImage(authManager.getBearerToken(), selectedItemId, body)
                    .enqueue(new Callback<ClothingItemSaveResponse>() {
                        @Override
                        public void onResponse(Call<ClothingItemSaveResponse> call, Response<ClothingItemSaveResponse> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null) {
                                if (response.body().item != null) populateForm(response.body().item);
                                loadItems();
                                showStatus(response.body().message, false);
                            } else {
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

    private void saveSizeChart() {
        if (selectedItemId == null) return;
        try {
            Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
            SizeChartUpdateRequest request = new SizeChartUpdateRequest();
            request.sizeChart = gson.fromJson(sizeChartInput.getText().toString(), type);
            setLoading(true);
            ApiClient.get().updateAdminSizeChart(authManager.getBearerToken(), selectedItemId, request)
                    .enqueue(new Callback<SizeChartUpdateResponse>() {
                        @Override
                        public void onResponse(Call<SizeChartUpdateResponse> call, Response<SizeChartUpdateResponse> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null) {
                                if (response.body().availableSizes != null) {
                                    sizesInput.setText(String.join(",", response.body().availableSizes));
                                }
                                showStatus(response.body().message, false);
                                loadItem(selectedItemId);
                            } else {
                                showStatus(parseError(response), true);
                            }
                        }

                        @Override
                        public void onFailure(Call<SizeChartUpdateResponse> call, Throwable t) {
                            setLoading(false);
                            showStatus("Network error: " + t.getMessage(), true);
                        }
                    });
        } catch (Exception e) {
            showStatus("Invalid size chart JSON", true);
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

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!loading);
        searchButton.setEnabled(!loading);
    }

    private void showStatus(String message, boolean isError) {
        statusText.setText(message == null ? "" : message);
        statusText.setTextColor(isError ? 0xFFB00020 : 0xFF388E3C);
        statusText.setVisibility(View.VISIBLE);
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

    private String textOrNull(EditText input) {
        String value = input.getText().toString().trim();
        return value.isEmpty() ? null : value;
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
