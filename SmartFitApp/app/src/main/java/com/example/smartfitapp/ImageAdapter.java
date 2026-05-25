package com.example.smartfitapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartfitapp.model.TryOnResult;
import com.example.smartfitapp.network.ApiClient;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged();
    }

    public interface OnPhotoClickListener {
        void onPhotoClick(TryOnResult result);
    }

    private final List<TryOnResult> items;
    private final Set<Long> selectedIds = new HashSet<>();
    private final OnSelectionChangedListener selectionChangedListener;
    private final OnPhotoClickListener photoClickListener;
    private boolean selectionMode = false;

    public ImageAdapter(List<TryOnResult> items, OnSelectionChangedListener selectionChangedListener,
                        OnPhotoClickListener photoClickListener) {
        this.items = items;
        this.selectionChangedListener = selectionChangedListener;
        this.photoClickListener = photoClickListener;
    }

    public void setItems(List<TryOnResult> newItems) {
        items.clear();
        items.addAll(newItems);
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        if (!selectionMode) selectedIds.clear();
        notifyDataSetChanged();
        selectionChangedListener.onSelectionChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void selectAll() {
        selectedIds.clear();
        for (TryOnResult item : items) {
            if (item.id != null) selectedIds.add(item.id);
        }
        notifyDataSetChanged();
        selectionChangedListener.onSelectionChanged();
    }

    public List<TryOnResult> getSelectedItems() {
        return items.stream()
                .filter(item -> item.id != null && selectedIds.contains(item.id))
                .toList();
    }

    public int getSelectedCount() {
        return selectedIds.size();
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
        TryOnResult item = items.get(position);

        Glide.with(holder.thumbnail.getContext())
                .load(ApiClient.fullImageUrl(item.resultImageUrl))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .fitCenter()
                .into(holder.thumbnail);

        String name = item.clothingItem != null && item.clothingItem.name != null
                ? item.clothingItem.name
                : "Virtual Try-On";
        holder.imageType.setText(name);
        holder.createdAt.setText(item.createdAt != null ? item.createdAt.substring(0, 10) : "");

        holder.selectCheckbox.setOnCheckedChangeListener(null);
        holder.selectCheckbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.selectCheckbox.setChecked(item.id != null && selectedIds.contains(item.id));
        holder.selectCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (item.id == null) return;
            if (isChecked) {
                selectedIds.add(item.id);
            } else {
                selectedIds.remove(item.id);
            }
            selectionChangedListener.onSelectionChanged();
        });
        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                holder.selectCheckbox.performClick();
            } else {
                photoClickListener.onPhotoClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView imageType, createdAt;
        CheckBox selectCheckbox;

        ViewHolder(View itemView) {
            super(itemView);
            thumbnail    = itemView.findViewById(R.id.thumbnail);
            imageType    = itemView.findViewById(R.id.imageType);
            createdAt    = itemView.findViewById(R.id.createdAt);
            selectCheckbox = itemView.findViewById(R.id.selectCheckbox);
        }
    }
}
