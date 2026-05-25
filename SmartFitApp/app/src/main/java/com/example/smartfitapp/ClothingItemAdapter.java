package com.example.smartfitapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartfitapp.model.ClothingItem;
import com.example.smartfitapp.network.ApiClient;

import java.util.List;

public class ClothingItemAdapter extends RecyclerView.Adapter<ClothingItemAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ClothingItem item);
    }

    private final List<ClothingItem> items;
    private final OnItemClickListener clickListener;

    public ClothingItemAdapter(List<ClothingItem> items, OnItemClickListener clickListener) {
        this.items = items;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_clothing, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClothingItem item = items.get(position);
        holder.nameText.setText(item.name);
        holder.categoryText.setText(item.category != null ? item.category : "");
        holder.genderText.setText(item.gender != null ? item.gender : "");

        String sizes = (item.availableSizes != null && !item.availableSizes.isEmpty())
                ? String.join(", ", item.availableSizes)
                : "N/A";
        holder.sizesText.setText("Sizes: " + sizes);

        Context ctx = holder.itemView.getContext();
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(ctx)
                    .load(ApiClient.fullImageUrl(item.imageUrl))
                    .fitCenter()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.thumbnail);
        } else {
            holder.thumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView nameText, categoryText, genderText, sizesText;

        ViewHolder(View v) {
            super(v);
            thumbnail = v.findViewById(R.id.thumbnail);
            nameText = v.findViewById(R.id.nameText);
            categoryText = v.findViewById(R.id.categoryText);
            genderText = v.findViewById(R.id.genderText);
            sizesText = v.findViewById(R.id.sizesText);
        }
    }
}
