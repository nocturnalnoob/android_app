package com.example.yolo_road;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        Log.d(TAG, "onCreate started");

        apiService = new ApiService();
        setupToolbar();
        initializeViews();
        loadAllResults();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detection Results");
        }
    }

    private void initializeViews() {
        // Initialize RecyclerView
        detectionsRecyclerView = findViewById(R.id.detectionsRecyclerView);
        detectionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DetectionAdapter();
        adapter.setOnDetectionClickListener(this);
        detectionsRecyclerView.setAdapter(adapter);

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::loadAllResults);

        // Initialize FAB
        fabNewScan = findViewById(R.id.fabNewScan);
        if (fabNewScan != null) {
            fabNewScan.setOnClickListener(v -> startNewScan());
        }
    }

    private void loadAllResults() {
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
    }

    @Override
    public void onDetectionClick(Detection detection) {
        // Show detection details in a fragment
        DetectionDetailFragment fragment = DetectionDetailFragment.newInstance(detection);
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error: " + message);
    }

    private void startNewScan() {
        finish();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}