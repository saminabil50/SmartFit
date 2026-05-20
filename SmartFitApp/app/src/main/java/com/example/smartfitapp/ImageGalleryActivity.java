package com.example.smartfitapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.ImageItem;
import com.example.smartfitapp.model.ImageListResponse;
import com.example.smartfitapp.model.MessageResponse;
import com.example.smartfitapp.network.ApiClient;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageGalleryActivity extends AppCompatActivity implements ImageAdapter.OnDeleteListener {

    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyText;
    private Button uploadButton;
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
        uploadButton = findViewById(R.id.uploadButton);

        adapter = new ImageAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        uploadButton.setOnClickListener(v ->
                startActivity(new Intent(this, ImageUploadActivity.class)));

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

        ApiClient.get().getMyImages(authManager.getBearerToken()).enqueue(new Callback<ImageListResponse>() {
            @Override
            public void onResponse(Call<ImageListResponse> call, Response<ImageListResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                    adapter.setItems(response.body().items);
                    emptyText.setVisibility(response.body().items.isEmpty() ? View.VISIBLE : View.GONE);
                } else if (response.code() == 401) {
                    authManager.logout();
                    startActivity(new Intent(ImageGalleryActivity.this, LoginActivity.class));
                    finish();
                }
            }
            @Override
            public void onFailure(Call<ImageListResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                emptyText.setText("Failed to load images: " + t.getMessage());
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDelete(ImageItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Photo")
                .setMessage("Delete this photo permanently?")
                .setPositiveButton("Delete", (dialog, which) -> performDelete(item, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(ImageItem item, int position) {
        ApiClient.get().deleteImage(authManager.getBearerToken(), item.id).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful()) {
                    adapter.removeItem(position);
                    if (adapter.getItemCount() == 0) emptyText.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {}
        });
    }
}
