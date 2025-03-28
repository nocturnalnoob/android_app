package com.example.yolo_road;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

public class DetectionDetailFragment extends Fragment {
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
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            detection = getArguments().getParcelable(ARG_DETECTION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detection_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (detection == null) return;

        // Initialize views
        ImageView imageView = view.findViewById(R.id.detectionImageView);
        TextView classNameText = view.findViewById(R.id.classNameText);
        TextView confidenceText = view.findViewById(R.id.confidenceText);
        TextView timestampText = view.findViewById(R.id.timestampText);

        // Set detection details
        classNameText.setText("Class: " + detection.getClassName());
        confidenceText.setText(String.format("Confidence: %.2f%%", detection.getConfidence() * 100));
        timestampText.setText("Time: " + detection.getLocation());

        // Load image
        if (detection.getImageUrl() != null && !detection.getImageUrl().isEmpty()) {
            GlideUrl glideUrl = new GlideUrl(detection.getImageUrl(), new LazyHeaders.Builder()
                    .addHeader("apikey", ApiService.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + ApiService.SUPABASE_KEY)
                    .build());

            Glide.with(this)
                    .load(glideUrl)
                    .error(R.drawable.error_image)
                    .into(imageView);
        }
    }
} 