package com.example.yolo_road;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialButton btnReportPothole = findViewById(R.id.btnReportPothole);
        MaterialButton btnViewResults = findViewById(R.id.btnViewResults);
        MaterialCardView reportCard = findViewById(R.id.reportCard);
        MaterialCardView viewResultsCard = findViewById(R.id.viewResultsCard);

        // Button click listeners
        btnReportPothole.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting CameraActivity: " + e.getMessage());
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnViewResults.setOnClickListener(v -> {
            try {
                Log.d(TAG, "Starting ResultsActivity...");
                Intent intent = new Intent(MainActivity.this, ResultsActivity.class);
                intent.putExtra("VIEW_ALL_RESULTS", true);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting ResultsActivity: " + e.getMessage());
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Card click listeners
        reportCard.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting CameraActivity: " + e.getMessage());
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        viewResultsCard.setOnClickListener(v -> {
            try {
                Log.d(TAG, "Starting ResultsActivity...");
                Intent intent = new Intent(MainActivity.this, ResultsActivity.class);
                intent.putExtra("VIEW_ALL_RESULTS", true);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting ResultsActivity: " + e.getMessage());
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}