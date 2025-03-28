package com.example.yolo_road;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "DetectionAdapter";
    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    private List<Detection> detections = new ArrayList<>();
    private boolean isLoadingVisible = false;
    private OnDetectionClickListener clickListener;

    public interface OnDetectionClickListener {
        void onDetectionClick(Detection detection);
    }

    public void setOnDetectionClickListener(OnDetectionClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return position == detections.size() ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_detection, parent, false);
        return new DetectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DetectionViewHolder && position < detections.size()) {
            Detection detection = detections.get(position);
            ((DetectionViewHolder) holder).bind(detection);
        }
    }

    @Override
    public int getItemCount() {
        return detections.size() + (isLoadingVisible ? 1 : 0);
    }

    public void setDetections(List<Detection> newDetections) {
        detections.clear();
        detections.addAll(newDetections);
        notifyDataSetChanged();
    }

    public void addDetections(List<Detection> newDetections) {
        int startPosition = detections.size();
        detections.addAll(newDetections);
        notifyItemRangeInserted(startPosition, newDetections.size());
    }

    public List<Detection> getDetections() {
        return new ArrayList<>(detections);
    }

    public void showLoading() {
        if (!isLoadingVisible) {
            isLoadingVisible = true;
            notifyItemInserted(detections.size());
        }
    }

    public void hideLoading() {
        if (isLoadingVisible) {
            isLoadingVisible = false;
            notifyItemRemoved(detections.size());
        }
    }

    class DetectionViewHolder extends RecyclerView.ViewHolder {
        private final TextView classNameTextView;
        private final TextView confidenceTextView;
        private final TextView locationTextView;
        private final MaterialCardView cardView;

        DetectionViewHolder(View itemView) {
            super(itemView);
            classNameTextView = itemView.findViewById(R.id.tvDetectionClass);
            confidenceTextView = itemView.findViewById(R.id.tvConfidence);
            locationTextView = itemView.findViewById(R.id.tvLocation);
            cardView = (MaterialCardView) itemView;
        }

        void bind(Detection detection) {
            try {
                if (classNameTextView == null || confidenceTextView == null || locationTextView == null) {
                    Log.e(TAG, "One or more TextViews are null");
                    return;
                }

                classNameTextView.setText(detection.getClassName());
                confidenceTextView.setText(String.format(Locale.US, "Confidence: %.2f%%",
                        detection.getConfidence() * 100));
                locationTextView.setText(detection.getLocation());

                cardView.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onDetectionClick(detection);
                    }
                });

                // Card appearance animation
                cardView.setAlpha(0f);
                cardView.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .start();

            } catch (Exception e) {
                Log.e(TAG, "Error binding detection: " + e.getMessage(), e);
            }
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
    }
}