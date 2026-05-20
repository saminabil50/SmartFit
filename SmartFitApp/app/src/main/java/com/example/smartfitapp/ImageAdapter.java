package com.example.smartfitapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartfitapp.model.ImageItem;
import com.example.smartfitapp.network.ApiClient;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(ImageItem item, int position);
    }

    private final List<ImageItem> items;
    private final OnDeleteListener deleteListener;

    public ImageAdapter(List<ImageItem> items, OnDeleteListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    public void setItems(List<ImageItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageItem item = items.get(position);

        Glide.with(holder.thumbnail.getContext())
                .load(ApiClient.fullImageUrl(item.imageUrl))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.thumbnail);

        holder.imageType.setText(item.imageType != null ? item.imageType.replace("_", " ") : "");
        holder.createdAt.setText(item.createdAt != null ? item.createdAt.substring(0, 10) : "");

        holder.deleteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) deleteListener.onDelete(item, pos);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView imageType, createdAt;
        ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            thumbnail    = itemView.findViewById(R.id.thumbnail);
            imageType    = itemView.findViewById(R.id.imageType);
            createdAt    = itemView.findViewById(R.id.createdAt);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
