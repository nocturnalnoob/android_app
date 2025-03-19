package com.example.yolo_road;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DetectionAdapter extends RecyclerView.Adapter<DetectionAdapter.DetectionViewHolder> {
    private List<Detection> detections = new ArrayList<>();
    private OnDetectionClickListener clickListener;

    // Interface for click listener
    public interface OnDetectionClickListener {
        void onDetectionClick(Detection detection);
    }

    public void setOnDetectionClickListener(OnDetectionClickListener listener) {
        this.clickListener = listener;
    }

    public void setDetections(List<Detection> detections) {
        this.detections = detections;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DetectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_detection, parent, false);
        return new DetectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DetectionViewHolder holder, int position) {
        Detection detection = detections.get(position);
        holder.tvDetectionClass.setText(detection.getClassName());
        holder.tvConfidence.setText(String.format("Confidence: %.2f%%", detection.getConfidence() * 100));
        holder.tvLocation.setText(detection.getLocation());

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDetectionClick(detection);
            }
        });
    }

    @Override
    public int getItemCount() {
        return detections.size();
    }

    static class DetectionViewHolder extends RecyclerView.ViewHolder {
        TextView tvDetectionClass;
        TextView tvConfidence;
        TextView tvLocation;

        DetectionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDetectionClass = itemView.findViewById(R.id.tvDetectionClass);
            tvConfidence = itemView.findViewById(R.id.tvConfidence);
            tvLocation = itemView.findViewById(R.id.tvLocation);
        }
    }
}