package com.example.smartfitapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfitapp.model.MeasurementResponse;

import java.util.List;

public class MeasurementAdapter extends RecyclerView.Adapter<MeasurementAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(MeasurementResponse measurement);
    }

    private final List<MeasurementResponse> items;
    private final OnDeleteListener deleteListener;

    public MeasurementAdapter(List<MeasurementResponse> items, OnDeleteListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_measurement, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MeasurementResponse m = items.get(position);
        holder.heightText.setText("Height used: " + m.heightCmUsed + " cm");
        holder.chestText.setText("Chest: " + m.chest + " cm");
        holder.waistText.setText("Waist: " + m.waist + " cm");
        holder.hipText.setText("Hip: " + m.hip + " cm");
        holder.shoulderText.setText("Shoulder: " + m.shoulderWidth + " cm");
        holder.inseamText.setText("Inseam: " + m.inseam + " cm");
        holder.confidenceText.setText("Confidence: " + String.format("%.0f%%", (m.confidenceScore != null ? m.confidenceScore : 0) * 100));
        holder.deleteButton.setOnClickListener(v -> deleteListener.onDelete(m));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView heightText, chestText, waistText, hipText, shoulderText, inseamText, confidenceText;
        ImageButton deleteButton;

        ViewHolder(View v) {
            super(v);
            heightText = v.findViewById(R.id.heightText);
            chestText = v.findViewById(R.id.chestText);
            waistText = v.findViewById(R.id.waistText);
            hipText = v.findViewById(R.id.hipText);
            shoulderText = v.findViewById(R.id.shoulderText);
            inseamText = v.findViewById(R.id.inseamText);
            confidenceText = v.findViewById(R.id.confidenceText);
            deleteButton = v.findViewById(R.id.deleteButton);
        }
    }
}
