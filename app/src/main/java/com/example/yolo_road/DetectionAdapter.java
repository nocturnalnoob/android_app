package com.example.yolo_road;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
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
        // Loading ViewHolder doesn't need any binding as Lottie animation is auto-playing
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
        private final LottieAnimationView detectionIconAnimation;

        DetectionViewHolder(View itemView) {
            super(itemView);
            classNameTextView = itemView.findViewById(R.id.classNameTextView);
            confidenceTextView = itemView.findViewById(R.id.confidenceTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            cardView = itemView.findViewById(R.id.detectionCardView);
            detectionIconAnimation = itemView.findViewById(R.id.detectionIconAnimation);
        }

        void bind(Detection detection) {
            try {
                classNameTextView.setText(detection.getClassName());
                confidenceTextView.setText(String.format(Locale.US, "Confidence: %.2f%%",
                        detection.getConfidence() * 100));
                locationTextView.setText(detection.getLocation());

                // Set animation based on detection class

                detectionIconAnimation.setAnimation(R.raw.detection_icon);


                // Configure and start animation
                detectionIconAnimation.setRepeatCount(LottieDrawable.INFINITE);
                if (!detectionIconAnimation.isAnimating()) {
                    detectionIconAnimation.playAnimation();
                }

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
                Log.e(TAG, "Error binding detection: " + e.getMessage());
            }
        }

        void stopAnimation() {
            if (detectionIconAnimation != null && detectionIconAnimation.isAnimating()) {
                detectionIconAnimation.cancelAnimation();
            }
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        private final LottieAnimationView loadingAnimation;

        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            loadingAnimation = itemView.findViewById(R.id.loadingAnimation);

            // Configure and start loading animation
            if (loadingAnimation != null) {
                loadingAnimation.setRepeatCount(LottieDrawable.INFINITE);
                if (!loadingAnimation.isAnimating()) {
                    loadingAnimation.playAnimation();
                }
            }
        }

        void stopAnimation() {
            if (loadingAnimation != null && loadingAnimation.isAnimating()) {
                loadingAnimation.cancelAnimation();
            }
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder instanceof DetectionViewHolder) {
            DetectionViewHolder detectionHolder = (DetectionViewHolder) holder;
            LottieAnimationView animation = detectionHolder.detectionIconAnimation;
            if (animation != null && !animation.isAnimating()) {
                animation.playAnimation();
            }
        } else if (holder instanceof LoadingViewHolder) {
            LoadingViewHolder loadingHolder = (LoadingViewHolder) holder;
            if (loadingHolder.loadingAnimation != null && !loadingHolder.loadingAnimation.isAnimating()) {
                loadingHolder.loadingAnimation.playAnimation();
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof DetectionViewHolder) {
            ((DetectionViewHolder) holder).stopAnimation();
        } else if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).stopAnimation();
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof DetectionViewHolder) {
            ((DetectionViewHolder) holder).stopAnimation();
        } else if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).stopAnimation();
        }
    }
}