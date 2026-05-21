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
import com.example.smartfitapp.model.TryOnResult;
import com.example.smartfitapp.network.ApiClient;

import java.util.List;

public class TryOnResultAdapter extends RecyclerView.Adapter<TryOnResultAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(TryOnResult result, int position);
    }

    private final List<TryOnResult> items;
    private final OnDeleteListener deleteListener;

    public TryOnResultAdapter(List<TryOnResult> items, OnDeleteListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    public void setItems(List<TryOnResult> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void addFirst(TryOnResult result) {
        items.add(0, result);
        notifyItemInserted(0);
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
                .inflate(R.layout.item_tryon_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TryOnResult result = items.get(position);
        String itemName = result.clothingItem != null ? result.clothingItem.name : "Clothing item";
        holder.itemName.setText(itemName);
        holder.createdAt.setText(result.createdAt != null && result.createdAt.length() >= 10
                ? result.createdAt.substring(0, 10)
                : "");
        holder.statusText.setText(result.status != null ? result.status : "");

        Glide.with(holder.thumbnail.getContext())
                .load(ApiClient.fullImageUrl(result.resultImageUrl))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.thumbnail);

        holder.deleteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) deleteListener.onDelete(result, pos);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView itemName, createdAt, statusText;
        ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            itemName = itemView.findViewById(R.id.itemName);
            createdAt = itemView.findViewById(R.id.createdAt);
            statusText = itemView.findViewById(R.id.statusText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
