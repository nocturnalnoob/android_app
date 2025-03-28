package com.example.yolo_road;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

public class DetectionDetailFragment extends Fragment {
    private static final String TAG = "DetectionDetailFragment";
    private static final String ARG_DETECTION = "detection";
    private Detection detection;

    public static DetectionDetailFragment newInstance(Detection detection) {
        DetectionDetailFragment fragment = new DetectionDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_DETECTION, detection);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                detection = getArguments().getParcelable(ARG_DETECTION);
                if (detection == null) {
                    Log.e(TAG, "Detection is null");
                    showError("Invalid detection data");
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            showError("Error initializing fragment");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_detection_detail, container, false);
        } catch (Exception e) {
            Log.e(TAG, "Error creating view: " + e.getMessage(), e);
            showError("Error creating view");
            return null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        try {
            super.onViewCreated(view, savedInstanceState);

            if (detection == null) {
                Log.e(TAG, "Detection is null in onViewCreated");
                showError("Invalid detection data");
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
                return;
            }

            // Initialize views
            ImageView imageView = view.findViewById(R.id.detectionImageView);
            TextView classNameText = view.findViewById(R.id.classNameText);
            TextView confidenceText = view.findViewById(R.id.confidenceText);
            TextView timestampText = view.findViewById(R.id.timestampText);

            if (imageView == null || classNameText == null || confidenceText == null || timestampText == null) {
                Log.e(TAG, "One or more views not found");
                showError("Error initializing views");
                return;
            }

            // Set detection details
            classNameText.setText("Class: " + detection.getClassName());
            confidenceText.setText(String.format("Confidence: %.2f%%", detection.getConfidence() * 100));
            timestampText.setText("Time: " + detection.getLocation());

            // Load image
            String imageUrl = detection.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                            .addHeader("apikey", ApiService.SUPABASE_KEY)
                            .addHeader("Authorization", "Bearer " + ApiService.SUPABASE_KEY)
                            .build());

                    Glide.with(this)
                            .load(glideUrl)
                            .error(R.drawable.error_image)
                            .into(imageView);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image: " + e.getMessage(), e);
                    showError("Error loading image");
                }
            } else {
                Log.w(TAG, "No image URL available");
                imageView.setImageResource(R.drawable.error_image);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated: " + e.getMessage(), e);
            showError("Error displaying detection details");
        }
    }

    private void showError(String message) {
        try {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error: " + message);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing error message: " + e.getMessage(), e);
        }
    }
} 