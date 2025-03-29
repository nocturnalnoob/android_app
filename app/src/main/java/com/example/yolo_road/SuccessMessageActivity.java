package com.example.yolo_road;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class SuccessMessageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success_message);

        // Find views
        View animationView = findViewById(R.id.animationView);
        View tvSuccessMessage = findViewById(R.id.tvSuccessMessage);
        View tvSubMessage = findViewById(R.id.tvSubMessage);
        MaterialButton btnBackToHome = findViewById(R.id.btnBackToHome);

        // Initially hide the views
        tvSuccessMessage.setAlpha(0f);
        tvSubMessage.setAlpha(0f);
        btnBackToHome.setAlpha(0f);

        // Animate the views
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Fade in the messages
            tvSuccessMessage.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start();

            tvSubMessage.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(200)
                    .start();

            btnBackToHome.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(400)
                    .start();
        }, 1500); // Wait for the Lottie animation to play

        // Set up button click
        btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Go back to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}