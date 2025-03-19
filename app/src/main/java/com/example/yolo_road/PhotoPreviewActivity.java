package com.example.yolo_road;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class PhotoPreviewActivity extends AppCompatActivity {
    private static final String TAG = "PhotoPreviewActivity";

    private ImageView previewImageView;
    private MaterialButton btnSendToServer;
    private MaterialButton btnDiscard;
    private Uri imageUri;
    private AlertDialog processingDialog;
    private File tempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_preview);

        // Setup toolbar
        setupToolbar();

        // Initialize views
        initializeViews();

        // Get image URI from intent
        imageUri = getIntent().getData();
        if (imageUri == null) {
            showError("No image provided");
            finish();
            return;
        }

        // Load and display the image
        loadImage();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Preview");
        }
    }

    private void initializeViews() {
        previewImageView = findViewById(R.id.previewImageView);
        btnSendToServer = findViewById(R.id.btnSendToServer);
        btnDiscard = findViewById(R.id.btnDiscard);

        btnSendToServer.setOnClickListener(v -> sendImageToServer());
        btnDiscard.setOnClickListener(v -> discardImage());
    }

    private void loadImage() {
        Log.d(TAG, "Loading image from URI: " + imageUri);

        Glide.with(this)
                .load(imageUri)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Failed to load image", e);
                        showError("Failed to load image");
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        Log.d(TAG, "Image loaded successfully");
                        return false;
                    }
                })
                .into(previewImageView);
    }

    private void sendImageToServer() {
        if (imageUri == null) {
            showError("No image available");
            return;
        }

        showProcessingDialog();

        try {
            // Create temporary file from URI
            tempFile = createTempFileFromUri(imageUri);
            if (tempFile == null) {
                throw new Exception("Failed to create temporary file");
            }

            // Create API service instance
            ApiService apiService = new ApiService();

            // Upload the file
            apiService.uploadImage(this, tempFile, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        dismissProcessingDialog();
                        Intent intent = new Intent(PhotoPreviewActivity.this, SuccessMessageActivity.class);
                        startActivity(intent);
                        finish();
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
//                        navigateToResults(response);
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        dismissProcessingDialog();
                        showError("Upload failed: " + error);
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing image for upload", e);
            dismissProcessingDialog();
            showError("Error preparing image: " + e.getMessage());
        }
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            // Create temporary file
            File tempFile = new File(getCacheDir(), "temp_" + UUID.randomUUID().toString() + ".jpg");

            // Copy content from Uri to temporary file
            try (InputStream is = getContentResolver().openInputStream(uri);
                 OutputStream os = new FileOutputStream(tempFile)) {

                if (is == null) {
                    throw new Exception("Could not open input stream");
                }

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
                return tempFile;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating temp file", e);
            return null;
        }
    }

    private void showProcessingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.dialog_processing);
        builder.setCancelable(false);
        processingDialog = builder.create();
        processingDialog.show();
    }

    private void dismissProcessingDialog() {
        if (processingDialog != null && processingDialog.isShowing()) {
            processingDialog.dismiss();
        }
    }

//    private void navigateToResults(JSONObject response) {
//        Intent intent = new Intent(this, ResultsActivity.class);
//        intent.setData(imageUri);
//        intent.putExtra("DETECTION_RESULTS", response.toString());
//        startActivity(intent);
//        finish();
//    }

    private void discardImage() {
        if (imageUri != null) {
            try {
                getContentResolver().delete(imageUri, null, null);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting image", e);
            }
        }
        startNewScan();
    }

    private void startNewScan() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up temporary file
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }
}