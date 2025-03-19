package com.example.yolo_road;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultsActivity extends AppCompatActivity implements DetectionAdapter.OnDetectionClickListener {
    private static final String TAG = "ResultsActivity";
    private ImageView resultImageView;
    private RecyclerView detectionsRecyclerView;
    private LottieAnimationView loadingAnimation;
    private LottieAnimationView emptyStateAnimation;
    private View emptyStateView;
    private DetectionAdapter adapter;
    private ApiService apiService;
    private Map<String, String> resultImageMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_results);
            Log.d(TAG, "onCreate started");

            // Initialize ApiService
            apiService = new ApiService();

            // Setup toolbar
            setupToolbar();

            // Initialize views
            initializeViews();

            // Check if we're coming from camera or viewing all results
            boolean viewAllResults = getIntent().getBooleanExtra("VIEW_ALL_RESULTS", false);
            Log.d(TAG, "viewAllResults: " + viewAllResults);

            if (viewAllResults) {
                loadAllResults();
            } else {
                // Handle single result from camera
                String detectionResults = getIntent().getStringExtra("DETECTION_RESULTS");
                if (detectionResults != null) {
                    processSingleResult(detectionResults);
                } else {
                    showError("No detection results available");
                    showEmptyState("No detection results found", true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            showError("Error initializing: " + e.getMessage());
        }
    }

    private void setupToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Detection Results");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar: " + e.getMessage(), e);
        }
    }

    private void initializeViews() {
        try {
            Log.d(TAG, "Initializing views");
            resultImageView = findViewById(R.id.resultImageView);
            detectionsRecyclerView = findViewById(R.id.detectionsRecyclerView);
            loadingAnimation = findViewById(R.id.loadingAnimation);
            emptyStateAnimation = findViewById(R.id.emptyStateAnimation);
            emptyStateView = findViewById(R.id.emptyStateView);
            FloatingActionButton fabNewScan = findViewById(R.id.fabNewScan);

            // Setup RecyclerView
            detectionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new DetectionAdapter();
            adapter.setOnDetectionClickListener(this);
            detectionsRecyclerView.setAdapter(adapter);

            // Setup FAB click listener
            fabNewScan.setOnClickListener(v -> startNewScan());

            // Initially hide loading and empty state
            loadingAnimation.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.GONE);

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            throw e;
        }
    }

    private void loadAllResults() {
        try {
            Log.d(TAG, "Loading all results");
            showLoading(true);

            apiService.getAllResults(new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    Log.d(TAG, "Got response: " + response.toString());
                    runOnUiThread(() -> {
                        try {
                            // Process the results array
                            JSONArray results = response.getJSONArray("results");
                            if (results.length() > 0) {
                                // Process all results
                                List<Detection> allDetections = new ArrayList<>();

                                // Get the most recent result's image for display
                                JSONObject latestResult = results.getJSONObject(0);
                                String resultImageUrl = latestResult.getString("result_image_url");

                                // Load the image from the most recent result
                                loadDetectionImage(resultImageUrl);

                                // Collect all detections from all results
                                for (int i = 0; i < results.length(); i++) {
                                    JSONObject resultObj = results.getJSONObject(i);
                                    if (resultObj.has("detections")) {
                                        JSONArray detections = resultObj.getJSONArray("detections");
                                        String timestamp = resultObj.optString("timestamp", "Unknown time");

                                        // Store image URL for this timestamp
                                        String imageUrl = resultObj.optString("result_image_url", "");
                                        if (!imageUrl.isEmpty()) {
                                            resultImageMap.put(timestamp, imageUrl);
                                        }

                                        for (int j = 0; j < detections.length(); j++) {
                                            JSONObject detection = detections.getJSONObject(j);
                                            String className = detection.optString("class_name", "Unknown");
                                            double confidence = detection.optDouble("confidence", 0.0);

                                            allDetections.add(new Detection(className, confidence, timestamp));
                                        }
                                    }
                                }

                                // Update the adapter with all detections
                                adapter.setDetections(allDetections);

                                // Check if we got any detections
                                if (allDetections.isEmpty()) {
                                    showEmptyState("No objects detected", false);
                                } else {
                                    showEmptyState("", false);
                                }

                                showLoading(false);
                            } else {
                                showLoading(false);
                                showEmptyState("No detection results found", true);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing results: " + e.getMessage(), e);
                            showLoading(false);
                            showError("Error processing results: " + e.getMessage());
                            showEmptyState("Error processing results", true);
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "API call failed: " + error);
                    runOnUiThread(() -> {
                        showLoading(false);
                        showError("Failed to load results: " + error);
                        showEmptyState("Failed to load results", true);
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadAllResults: " + e.getMessage(), e);
            showLoading(false);
            showError("Error loading results: " + e.getMessage());
            showEmptyState("Error loading results", true);
        }
    }

    private void loadDetectionImage(String imageUrl) {
        try {
            Log.d(TAG, "Loading image from URL: " + imageUrl);
            String fullUrl = ApiService.SERVER_URL + imageUrl;
            Log.d(TAG, "Full URL: " + fullUrl);

            // Show loading animation for the image
            resultImageView.setVisibility(View.INVISIBLE);

            // Use Glide to load the image from URL
            Glide.with(this)
                    .load(fullUrl)
                    .error(R.drawable.error_image)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            resultImageView.setVisibility(View.VISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            resultImageView.setVisibility(View.VISIBLE);
                            return false;
                        }
                    })
                    .into(resultImageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading detection image: " + e.getMessage(), e);
            resultImageView.setVisibility(View.VISIBLE);
            showError("Error loading image: " + e.getMessage());
        }
    }

    private void processSingleResult(String detectionResults) {
        try {
            Log.d(TAG, "Processing single result: " + detectionResults);
            JSONObject resultData = new JSONObject(detectionResults);

            // Get the image path from the result
            String imagePath = resultData.optString("image_path", "");
            if (!imagePath.isEmpty()) {
                loadDetectionImage(imagePath);

                // Store image path for current scan
                resultImageMap.put("Current Scan", imagePath);
            } else {
                showError("No image path found in results");
            }

            // Update detections list if available
            if (resultData.has("detections")) {
                JSONArray detections = resultData.getJSONArray("detections");
                updateDetectionsList(detections);

                // Check if we got any detections
                if (detections.length() == 0) {
                    showEmptyState("No objects detected", false);
                } else {
                    showEmptyState("", false);
                }
            } else {
                showEmptyState("No detections found", false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing single result: " + e.getMessage(), e);
            showError("Error processing result: " + e.getMessage());
            showEmptyState("Error processing result", true);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateDetectionsList(JSONArray detections) {
        try {
            List<Detection> detectionList = new ArrayList<>();
            String timestamp = "Current Scan";

            for (int i = 0; i < detections.length(); i++) {
                JSONObject detection = detections.getJSONObject(i);
                String className = detection.optString("class_name", "Unknown");
                double confidence = detection.optDouble("confidence", 0.0);
                detectionList.add(new Detection(className, confidence, timestamp));
            }

            adapter.setDetections(detectionList);
        } catch (Exception e) {
            Log.e(TAG, "Error updating detections list: " + e.getMessage(), e);
            showError("Error updating detections: " + e.getMessage());
        }
    }

    @Override
    public void onDetectionClick(Detection detection) {
        // Handle click on a detection item
        String timestamp = detection.getLocation();
        String imageUrl = resultImageMap.get(timestamp);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Show loading animation
            showLoading(true);

            // Load the image
            loadDetectionImage(imageUrl);

            // Hide loading animation after a short delay
            loadingAnimation.postDelayed(() -> showLoading(false), 500);

            // Option 2: Open in full-screen image viewer
            // Uncomment to implement a separate image viewer activity
            /*
            Intent intent = new Intent(this, ImageViewerActivity.class);
            intent.putExtra("IMAGE_URL", ApiService.SERVER_URL + imageUrl);
            startActivity(intent);
            */
        } else {
            showError("Image not available for this detection");
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            loadingAnimation.setVisibility(View.VISIBLE);
            loadingAnimation.playAnimation();
        } else {
            loadingAnimation.pauseAnimation();
            loadingAnimation.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message, boolean show) {
        if (show) {
            emptyStateView.setVisibility(View.VISIBLE);
            emptyStateAnimation.playAnimation();

            // Set the message text if there's a TextView in the empty state layout
            TextView tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
            if (tvEmptyMessage != null && !message.isEmpty()) {
                tvEmptyMessage.setText(message);
            }
        } else {
            emptyStateView.setVisibility(View.GONE);
            emptyStateAnimation.pauseAnimation();
        }
    }

    private void showError(String message) {
        try {
            runOnUiThread(() -> {
                if (!isFinishing()) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error shown to user: " + message);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing error message: " + e.getMessage(), e);
        }
    }

    private void startNewScan() {
        try {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
            finish(); // Close this activity
        } catch (Exception e) {
            Log.e(TAG, "Error starting new scan: " + e.getMessage(), e);
            showError("Error starting new scan: " + e.getMessage());
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override

    protected void onDestroy() {
        try {
            super.onDestroy();
            if (loadingAnimation != null) {
                loadingAnimation.cancelAnimation();
            }
            if (emptyStateAnimation != null) {
                emptyStateAnimation.cancelAnimation();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }
    }
}