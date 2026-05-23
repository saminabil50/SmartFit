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
        holder.nameText.setText(item.name != null ? item.name : "Clothing item");
        holder.metaText.setText((item.category != null ? item.category : "") +
                (item.gender != null ? " - " + item.gender : ""));
        holder.selectedText.setVisibility(item.id != null && item.id.equals(selectedItemId)
                ? View.VISIBLE
                : View.GONE);

        Context context = holder.itemView.getContext();
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(ApiClient.fullImageUrl(item.imageUrl))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameText, metaText, selectedText;

        ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.itemImage);
            nameText = view.findViewById(R.id.itemName);
            metaText = view.findViewById(R.id.itemMeta);
            selectedText = view.findViewById(R.id.selectedText);
        }
    }
}
