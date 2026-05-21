package com.example.smartfitapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.smartfitapp.network.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;

public class ClothingItemDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clothing_item_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String name = getIntent().getStringExtra("item_name");
        String category = getIntent().getStringExtra("item_category");
        String gender = getIntent().getStringExtra("item_gender");
        String imageUrl = getIntent().getStringExtra("item_image_url");
        String sizes = getIntent().getStringExtra("item_sizes");

        toolbar.setTitle(name != null ? name : "Item Detail");

        TextView nameText = findViewById(R.id.nameText);
        TextView categoryText = findViewById(R.id.categoryText);
        TextView genderText = findViewById(R.id.genderText);
        TextView sizesText = findViewById(R.id.sizesText);
        ImageView itemImage = findViewById(R.id.itemImage);

        nameText.setText(name != null ? name : "");
        categoryText.setText("Category: " + (category != null ? category : "N/A"));
        genderText.setText("Gender: " + (gender != null ? gender : "N/A"));
        sizesText.setText(sizes != null ? sizes : "N/A");

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(ApiClient.fullImageUrl(imageUrl))
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(itemImage);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
