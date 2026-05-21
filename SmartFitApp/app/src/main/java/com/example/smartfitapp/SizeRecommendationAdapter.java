package com.example.smartfitapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfitapp.model.SizeRecommendation;

import java.util.List;

public class SizeRecommendationAdapter extends RecyclerView.Adapter<SizeRecommendationAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(SizeRecommendation recommendation, int position);
    }

    private final List<SizeRecommendation> items;
    private final OnDeleteListener deleteListener;

    public SizeRecommendationAdapter(List<SizeRecommendation> items, OnDeleteListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    public void setItems(List<SizeRecommendation> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void addFirst(SizeRecommendation recommendation) {
        items.add(0, recommendation);
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
                .inflate(R.layout.item_size_recommendation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SizeRecommendation item = items.get(position);
        String itemName = item.clothingItem != null ? item.clothingItem.name : "Clothing item";
        holder.itemName.setText(itemName);
        holder.sizeText.setText("Recommended size: " + safe(item.recommendedSize));
        holder.confidenceText.setText("Confidence: " + formatPercent(item.confidenceScore));
        holder.createdAt.setText(item.createdAt != null && item.createdAt.length() >= 10 ? item.createdAt.substring(0, 10) : "");
        holder.deleteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) deleteListener.onDelete(item, pos);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String formatPercent(Double value) {
        return String.format("%.0f%%", (value != null ? value : 0.0) * 100.0);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, sizeText, confidenceText, createdAt;
        ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            sizeText = itemView.findViewById(R.id.sizeText);
            confidenceText = itemView.findViewById(R.id.confidenceText);
            createdAt = itemView.findViewById(R.id.createdAt);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
