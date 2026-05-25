package com.example.smartfitapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.MessageResponse;
import com.example.smartfitapp.model.TryOnResult;
import com.example.smartfitapp.model.TryOnResultListResponse;
import com.example.smartfitapp.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageGalleryActivity extends AppCompatActivity implements ImageAdapter.OnSelectionChangedListener,
        ImageAdapter.OnPhotoClickListener {

    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyText, selectAllText;
    private LinearLayout bulkActionsLayout;
    private Button updateButton, deleteSelectedButton;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_gallery);

        authManager = new AuthManager(this);
        if (!authManager.isLoggedIn()) { finish(); return; }

        recyclerView = findViewById(R.id.recyclerView);
        progressBar  = findViewById(R.id.progressBar);
        emptyText    = findViewById(R.id.emptyText);
        updateButton = findViewById(R.id.updateButton);
        bulkActionsLayout = findViewById(R.id.bulkActionsLayout);
        selectAllText = findViewById(R.id.selectAllText);
        deleteSelectedButton = findViewById(R.id.deleteSelectedButton);
        selectAllText.setPaintFlags(selectAllText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        adapter = new ImageAdapter(new ArrayList<>(), this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        updateButton.setOnClickListener(v -> toggleUpdateMode());
        selectAllText.setOnClickListener(v -> adapter.selectAll());
        deleteSelectedButton.setOnClickListener(v -> confirmDeleteSelected());

        loadImages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImages();
    }

    private void loadImages() {
        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        ApiClient.get().getTryOnResults(authManager.getBearerToken()).enqueue(new Callback<TryOnResultListResponse>() {
            @Override
            public void onResponse(Call<TryOnResultListResponse> call, Response<TryOnResultListResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                    adapter.setItems(response.body().items);
                    emptyText.setVisibility(response.body().items.isEmpty() ? View.VISIBLE : View.GONE);
                    updateSelectionUi();
                } else if (response.code() == 401) {
                    authManager.logout();
                    startActivity(new Intent(ImageGalleryActivity.this, LoginActivity.class));
                    finish();
                }
            }
            @Override
            public void onFailure(Call<TryOnResultListResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                emptyText.setText("Failed to load images: " + t.getMessage());
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onSelectionChanged() {
        updateSelectionUi();
    }

    @Override
    public void onPhotoClick(TryOnResult result) {
        showPreview(result);
    }

    private void toggleUpdateMode() {
        adapter.setSelectionMode(!adapter.isSelectionMode());
    }

    private void updateSelectionUi() {
        boolean selectionMode = adapter.isSelectionMode();
        bulkActionsLayout.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        updateButton.setText(selectionMode ? "Done" : "Update");
        deleteSelectedButton.setEnabled(adapter.getSelectedCount() > 0);
    }

    private void confirmDeleteSelected() {
        List<TryOnResult> selectedItems = adapter.getSelectedItems();
        if (selectedItems.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Photos")
                .setMessage("Delete " + selectedItems.size() + " selected photo(s) permanently?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSelected(selectedItems))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSelected(List<TryOnResult> selectedItems) {
        progressBar.setVisibility(View.VISIBLE);
        deleteNext(new ArrayList<>(selectedItems), 0);
    }

    private void deleteNext(List<TryOnResult> selectedItems, int index) {
        if (index >= selectedItems.size()) {
            progressBar.setVisibility(View.GONE);
            adapter.setSelectionMode(false);
            loadImages();
            return;
        }
        TryOnResult item = selectedItems.get(index);
        ApiClient.get().deleteTryOnResult(authManager.getBearerToken(), item.id).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                deleteNext(selectedItems, index + 1);
            }
            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                deleteNext(selectedItems, index + 1);
            }
        });
    }

    private void showPreview(TryOnResult result) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);

        ImageView imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        content.addView(imageView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.round(420 * getResources().getDisplayMetrics().density)
        ));

        TextView details = new TextView(this);
        details.setTextColor(0xFF1C1B1F);
        details.setTextSize(14);
        details.setPadding(0, padding, 0, 0);
        details.setText(buildDetails(result));
        content.addView(details);

        Glide.with(this)
                .load(ApiClient.fullImageUrl(result.resultImageUrl))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .fitCenter()
                .into(imageView);

        new AlertDialog.Builder(this)
                .setTitle("Photo Details")
                .setView(content)
                .setPositiveButton("Close", null)
                .show();
    }

    private String buildDetails(TryOnResult result) {
        String itemName = result.clothingItem != null && result.clothingItem.name != null
                ? result.clothingItem.name
                : "Virtual Try-On";
        String category = result.clothingItem != null && result.clothingItem.category != null
                ? result.clothingItem.category
                : "";
        StringBuilder builder = new StringBuilder();
        builder.append("ID: ").append(result.id != null ? result.id : "").append("\n");
        builder.append("Item: ").append(itemName).append("\n");
        if (!category.isEmpty()) builder.append("Category: ").append(category).append("\n");
        builder.append("Status: ").append(result.status != null ? result.status : "").append("\n");
        builder.append("Created: ").append(result.createdAt != null ? result.createdAt : "").append("\n");
        if (result.confidenceScore != null) {
            builder.append("Confidence: ").append(result.confidenceScore).append("\n");
        }
        if (result.warnings != null && !result.warnings.isEmpty()) {
            builder.append("Warnings: ").append(String.join(", ", result.warnings)).append("\n");
        }
        builder.append("Image URL: ").append(result.resultImageUrl != null ? result.resultImageUrl : "");
        return builder.toString();
    }
}
