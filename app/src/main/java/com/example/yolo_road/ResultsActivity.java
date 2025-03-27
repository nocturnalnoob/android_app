package com.example.yolo_road;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieDrawable;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.bumptech.glide.load.model.LazyHeaders;

import okhttp3.OkHttpClient;

public class ResultsActivity extends AppCompatActivity implements DetectionAdapter.OnDetectionClickListener {
    private static final String TAG = "ResultsActivity";
    private static final String KEY_CURRENT_IMAGE = "current_image";
    private static final String KEY_DETECTIONS = "detections";
    private static final int PAGE_SIZE = 20;
    private ExtendedFloatingActionButton fabnewScan;
    private ImageView resultImageView;
    private RecyclerView detectionsRecyclerView;
    private LottieAnimationView loadingAnimation;
    private LottieAnimationView emptyStateAnimation;
    private View emptyStateView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DetectionAdapter adapter;
    private ApiService apiService;
    private final Map<String, String> resultImageMap = new HashMap<>();
    private String currentImageUrl;
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_results);
            Log.d(TAG, "onCreate started");

            apiService = new ApiService();
            setupToolbar();
            initializeViews();

            if (savedInstanceState != null) {
                restoreState(savedInstanceState);
            } else {
                boolean viewAllResults = getIntent().getBooleanExtra("VIEW_ALL_RESULTS", false);
                Log.d(TAG, "viewAllResults: " + viewAllResults);

                if (viewAllResults) {
                    loadAllResults();
                } else {
                    String detectionResults = getIntent().getStringExtra("DETECTION_RESULTS");
                    if (detectionResults != null) {
                        processSingleResult(detectionResults);
                    } else {
                        showError("No detection results available");
                        showEmptyState("No detection results found", true);
                    }
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

            // Initialize all view references
            resultImageView = findViewById(R.id.resultImageView);
            detectionsRecyclerView = findViewById(R.id.detectionsRecyclerView);
            loadingAnimation = findViewById(R.id.loadingAnimation);
            emptyStateAnimation = findViewById(R.id.emptyStateAnimation);
            emptyStateView = findViewById(R.id.emptyStateView);
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            ExtendedFloatingActionButton fabNewScan = findViewById(R.id.fabNewScan); // Added this line

            // Set up RecyclerView
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            detectionsRecyclerView.setLayoutManager(layoutManager);
            adapter = new DetectionAdapter();
            adapter.setOnDetectionClickListener(this);
            detectionsRecyclerView.setAdapter(adapter);
            setupRecyclerViewPagination(layoutManager);

            // Configure SwipeRefreshLayout
            swipeRefreshLayout.setOnRefreshListener(() -> {
                currentPage = 0;
                isLastPage = false;
                loadAllResults();
            });
            swipeRefreshLayout.setColorSchemeResources(R.color.purple_500); // Optional: Add refresh colors

            // Set up FAB
            if (fabNewScan != null) {
                fabNewScan.setOnClickListener(v -> startNewScan());
            } else {
                Log.e(TAG, "FAB not found in layout");
            }

            // Set initial visibility states
            loadingAnimation.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.GONE);

            // Configure Lottie animations if needed
            if (loadingAnimation != null) {
                loadingAnimation.setRepeatCount(LottieDrawable.INFINITE);
            }
            if (emptyStateAnimation != null) {
                emptyStateAnimation.setRepeatCount(LottieDrawable.INFINITE);
            }

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            throw e;
        }
    }

    private void setupRecyclerViewPagination(LinearLayoutManager layoutManager) {
        detectionsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        loadMoreResults();
                    }
                }
            }
        });
    }

    private void loadAllResults() {
        try {
            Log.d(TAG, "Loading initial results");
            currentPage = 0;
            isLastPage = false;
            showLoading(true);

            apiService.getAllResults(new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        try {
                            JSONArray results = response.getJSONArray("results");
                            processResults(results, true);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing results: " + e.getMessage());
                            showError("Error processing results: " + e.getMessage());
                            showEmptyState("Error processing results", true);
                        } finally {
                            showLoading(false);
                            if (swipeRefreshLayout.isRefreshing()) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "API call failed: " + error);
                    runOnUiThread(() -> {
                        showLoading(false);
                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        showError("Failed to load results: " + error);
                        showEmptyState("Failed to load results", true);
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadAllResults: " + e.getMessage());
            showLoading(false);
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            showError("Error loading results: " + e.getMessage());
        }
    }

    private void loadMoreResults() {
        if (isLoading || isLastPage) return;

        isLoading = true;
        adapter.showLoading(); // Show loading footer
        Log.d(TAG, "Loading more results, page: " + currentPage);

        apiService.getResultsPage(currentPage, PAGE_SIZE, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        adapter.hideLoading(); // Hide loading footer
                        JSONArray results = response.getJSONArray("results");
                        processResults(results, false);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing more results: " + e.getMessage());
                        showError("Error loading more results: " + e.getMessage());
                    } finally {
                        isLoading = false;
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to load more results: " + error);
                runOnUiThread(() -> {
                    adapter.hideLoading(); // Hide loading footer
                    isLoading = false;
                    showError("Failed to load more results: " + error);
                });
            }
        });
    }
    private void processResults(JSONArray results, boolean isFirstPage) {
        try {
            List<Detection> detections = new ArrayList<>();

            for (int i = 0; i < results.length(); i++) {
                JSONObject resultObj = results.getJSONObject(i);
                if (resultObj.has("detections")) {
                    JSONArray detectionsArray = new JSONArray(resultObj.getString("detections"));
                    String timestamp = resultObj.optString("timestamp", "Unknown time");
                    String imageUrl = resultObj.optString("result_image_url", "");

                    if (!imageUrl.isEmpty()) {
                        resultImageMap.put(timestamp, imageUrl);
                    }

                    if (isFirstPage && i == 0) {
                        loadDetectionImage(imageUrl);
                    }

                    for (int j = 0; j < detectionsArray.length(); j++) {
                        JSONObject detection = detectionsArray.getJSONObject(j);
                        String className = detection.optString("class_name", "Unknown");
                        double confidence = detection.optDouble("confidence", 0.0);
                        detections.add(new Detection(className, confidence, timestamp));
                    }
                }
            }

            if (isFirstPage) {
                adapter.setDetections(detections);
            } else {
                adapter.addDetections(detections);
            }

            isLastPage = results.length() < PAGE_SIZE;
            if (!isLastPage) {
                currentPage++;
            }

            if (isFirstPage && detections.isEmpty()) {
                showEmptyState("No detection results found", true);
            } else {
                showEmptyState("", false);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing results: " + e.getMessage());
            showError("Error processing results: " + e.getMessage());
        }
    }

    private void loadDetectionImage(String imageUrl) {
        try {
            Log.d(TAG, "Loading image from URL: " + imageUrl);
            currentImageUrl = imageUrl;

            GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                    .addHeader("apikey", ApiService.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + ApiService.SUPABASE_KEY)
                    .build());

            resultImageView.setVisibility(View.INVISIBLE);

            Glide.with(this)
                    .load(glideUrl)
                    .error(R.drawable.error_image)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            resultImageView.setVisibility(View.VISIBLE);
                            showError("Failed to load image: " + (e != null ? e.getMessage() : "unknown error"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
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

            String imagePath = resultData.optString("result_image_url", "");
            if (!imagePath.isEmpty()) {
                loadDetectionImage(imagePath);
                resultImageMap.put("Current Scan", imagePath);
            } else {
                showError("No image path found in results");
            }

            if (resultData.has("detections")) {
                JSONArray detections = resultData.getJSONArray("detections");
                updateDetectionsList(detections);
                showEmptyState(detections.length() == 0 ? "No objects detected" : "", detections.length() == 0);
            } else {
                showEmptyState("No detections found", true);
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
        String timestamp = detection.getLocation();
        String imageUrl = resultImageMap.get(timestamp);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            showLoading(true);
            loadDetectionImage(imageUrl);
            loadingAnimation.postDelayed(() -> showLoading(false), 500);
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
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error starting new scan: " + e.getMessage(), e);
            showError("Error starting new scan: " + e.getMessage());
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_IMAGE, currentImageUrl);
        outState.putParcelableArrayList(KEY_DETECTIONS, new ArrayList<>(adapter.getDetections()));
    }

    private void restoreState(Bundle savedInstanceState) {
        String savedImageUrl = savedInstanceState.getString(KEY_CURRENT_IMAGE);
        ArrayList<Detection> savedDetections = savedInstanceState.getParcelableArrayList(KEY_DETECTIONS);

        if (savedImageUrl != null) {
            loadDetectionImage(savedImageUrl);
        }
        if (savedDetections != null) {
            adapter.setDetections(savedDetections);
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
            if (!isFinishing()) {
                Glide.with(this).clear(resultImageView);
            }
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