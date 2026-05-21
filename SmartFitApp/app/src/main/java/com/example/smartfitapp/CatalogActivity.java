package com.example.smartfitapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfitapp.auth.AuthManager;
import com.example.smartfitapp.model.ClothingItem;
import com.example.smartfitapp.model.ClothingItemListResponse;
import com.example.smartfitapp.network.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CatalogActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 20;

    private AuthManager authManager;
    private ClothingItemAdapter adapter;
    private List<ClothingItem> items = new ArrayList<>();

    private Spinner categorySpinner, genderSpinner;
    private TextView emptyText, pageText;
    private Button prevButton, nextButton;
    private int currentPage = 0;
    private long totalItems = 0;

    private final String[] CATEGORIES = {"All", "tops", "bottoms", "shoes", "accessories", "outerwear"};
    private final String[] GENDERS = {"All", "male", "female", "unisex"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        authManager = new AuthManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        categorySpinner = findViewById(R.id.categorySpinner);
        genderSpinner = findViewById(R.id.genderSpinner);
        emptyText = findViewById(R.id.emptyText);
        pageText = findViewById(R.id.pageText);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        adapter = new ClothingItemAdapter(items, this::openItemDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CATEGORIES);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(catAdapter);

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, GENDERS);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPage = 0;
                loadItems();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        categorySpinner.setOnItemSelectedListener(filterListener);
        genderSpinner.setOnItemSelectedListener(filterListener);

        prevButton.setOnClickListener(v -> {
            if (currentPage > 0) { currentPage--; loadItems(); }
        });
        nextButton.setOnClickListener(v -> {
            if ((long)(currentPage + 1) * PAGE_SIZE < totalItems) { currentPage++; loadItems(); }
        });

        loadItems();
    }

    private void loadItems() {
        String category = categorySpinner.getSelectedItemPosition() == 0 ? null : CATEGORIES[categorySpinner.getSelectedItemPosition()];
        String gender = genderSpinner.getSelectedItemPosition() == 0 ? null : GENDERS[genderSpinner.getSelectedItemPosition()];

        ApiClient.get().getClothingItems(authManager.getBearerToken(), category, gender, currentPage, PAGE_SIZE)
                .enqueue(new Callback<ClothingItemListResponse>() {
                    @Override
                    public void onResponse(Call<ClothingItemListResponse> call, Response<ClothingItemListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ClothingItemListResponse body = response.body();
                            items.clear();
                            if (body.items != null) items.addAll(body.items);
                            adapter.notifyDataSetChanged();
                            totalItems = body.total;
                            pageText.setText("Page " + (currentPage + 1));
                            prevButton.setEnabled(currentPage > 0);
                            nextButton.setEnabled((long)(currentPage + 1) * PAGE_SIZE < totalItems);
                            emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                        } else {
                            Toast.makeText(CatalogActivity.this, "Failed to load catalog", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ClothingItemListResponse> call, Throwable t) {
                        Toast.makeText(CatalogActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openItemDetail(ClothingItem item) {
        Intent intent = new Intent(this, ClothingItemDetailActivity.class);
        intent.putExtra("item_id", item.id);
        intent.putExtra("item_name", item.name);
        intent.putExtra("item_category", item.category);
        intent.putExtra("item_gender", item.gender);
        intent.putExtra("item_image_url", item.imageUrl);
        if (item.availableSizes != null) {
            intent.putExtra("item_sizes", String.join(", ", item.availableSizes));
        }
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
