package com.example.smartfitapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartfitapp.model.ClothingItem;
import com.example.smartfitapp.network.ApiClient;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class TryOnClothingAdapter extends RecyclerView.Adapter<TryOnClothingAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ClothingItem item);
    }

    private final List<ClothingItem> items;
    private final OnItemClickListener clickListener;
    private Long selectedItemId;

    public TryOnClothingAdapter(List<ClothingItem> items, OnItemClickListener clickListener) {
        this.items = items;
        this.clickListener = clickListener;
    }

    public void setSelectedItemId(Long selectedItemId) {
        this.selectedItemId = selectedItemId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tryon_clothing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClothingItem item = items.get(position);
        boolean selected = item.id != null && item.id.equals(selectedItemId);
        holder.nameText.setText(item.name != null ? item.name : "Clothing item");
        holder.metaText.setText(buildMetaText(item));

        Context context = holder.itemView.getContext();
        holder.cardView.setStrokeColor(ContextCompat.getColor(
                context,
                selected ? R.color.primary : R.color.outline_variant
        ));
        holder.cardView.setStrokeWidth(selected ? dp(context, 2) : dp(context, 1));

        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(ApiClient.fullImageUrl(item.imageUrl))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .fitCenter()
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String buildMetaText(ClothingItem item) {
        String gender = item.gender != null ? item.gender.trim() : "";
        String category = item.category != null ? item.category.trim() : "";
        if (!gender.isEmpty() && !category.isEmpty()) return gender + " - " + category;
        if (!gender.isEmpty()) return gender;
        return category;
    }

    private int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView imageView;
        TextView nameText, metaText;

        ViewHolder(View view) {
            super(view);
            cardView = (MaterialCardView) view;
            imageView = view.findViewById(R.id.itemImage);
            nameText = view.findViewById(R.id.itemName);
            metaText = view.findViewById(R.id.itemMeta);
        }
    }
}
