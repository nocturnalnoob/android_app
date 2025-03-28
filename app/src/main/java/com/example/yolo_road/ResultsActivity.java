package com.example.yolo_road;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ResultsActivity extends AppCompatActivity implements DetectionAdapter.OnDetectionClickListener {
    private static final String TAG = "ResultsActivity";
    private RecyclerView detectionsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DetectionAdapter adapter;
    private ApiService apiService;
    private ExtendedFloatingActionButton fabNewScan;
    private View fragmentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_results);
            Log.d(TAG, "onCreate started");

            apiService = new ApiService();
            setupToolbar();
            initializeViews();
            loadAllResults();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            showError("Error initializing app: " + e.getMessage());
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
            // Initialize RecyclerView
            detectionsRecyclerView = findViewById(R.id.detectionsRecyclerView);
            detectionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new DetectionAdapter();
            adapter.setOnDetectionClickListener(this);
            detectionsRecyclerView.setAdapter(adapter);

            // Initialize SwipeRefreshLayout
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            swipeRefreshLayout.setOnRefreshListener(this::loadAllResults);

            // Initialize Fragment Container
            fragmentContainer = findViewById(R.id.fragmentContainer);
            if (fragmentContainer == null) {
                Log.e(TAG, "Fragment container not found in layout");
                return;
            }

            // Initialize FAB
            fabNewScan = findViewById(R.id.fabNewScan);
            if (fabNewScan != null) {
                fabNewScan.setOnClickListener(v -> startNewScan());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            showError("Error initializing views: " + e.getMessage());
        }
    }

    private void loadAllResults() {
        try {
            apiService.getAllResults(new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        try {
                            if (response == null) {
                                showError("No results available");
                                return;
                            }

                            JSONArray results = response.getJSONArray("results");
                            List<Detection> detections = new ArrayList<>();

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject resultObj = results.getJSONObject(i);
                                if (resultObj.has("detections")) {
                                    JSONArray detectionsArray = new JSONArray(resultObj.getString("detections"));
                                    String timestamp = resultObj.optString("timestamp", "Unknown time");
                                    String imageUrl = resultObj.optString("result_image_url", "");

                                    for (int j = 0; j < detectionsArray.length(); j++) {
                                        JSONObject detection = detectionsArray.getJSONObject(j);
                                        String className = detection.optString("class_name", "Unknown");
                                        double confidence = detection.optDouble("confidence", 0.0);
                                        detections.add(new Detection(className, confidence, timestamp, imageUrl));
                                    }
                                }
                            }

                            adapter.setDetections(detections);
                            if (detections.isEmpty()) {
                                showError("No detections found");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing results: " + e.getMessage(), e);
                            showError("Error processing results");
                        } finally {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        showError("Failed to load results: " + error);
                        swipeRefreshLayout.setRefreshing(false);
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadAllResults: " + e.getMessage(), e);
            showError("Error loading results: " + e.getMessage());
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onDetectionClick(Detection detection) {
        try {
            if (detection == null) {
                showError("Invalid detection data");
                return;
            }

            // Show detection details in a fragment
            DetectionDetailFragment fragment = DetectionDetailFragment.newInstance(detection);
            if (fragmentContainer != null) {
                fragmentContainer.setVisibility(View.VISIBLE);
                detectionsRecyclerView.setVisibility(View.GONE);
                if (fabNewScan != null) {
                    fabNewScan.hide();
                }
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Log.e(TAG, "Error showing detection details: " + e.getMessage(), e);
            showError("Error showing detection details");
        }
    }

    private void showError(String message) {
        try {
            runOnUiThread(() -> {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error: " + message);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing error message: " + e.getMessage(), e);
        }
    }

    private void startNewScan() {
        try {
            finish();
            Intent intent = new Intent(this, CameraActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting new scan: " + e.getMessage(), e);
            showError("Error starting new scan");
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
                if (fragmentContainer != null) {
                    fragmentContainer.setVisibility(View.GONE);
                    detectionsRecyclerView.setVisibility(View.VISIBLE);
                    if (fabNewScan != null) {
                        fabNewScan.show();
                    }
                }
            } else {
                super.onBackPressed();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling back press: " + e.getMessage(), e);
            super.onBackPressed();
        }
    }
}