package com.example.yolo_road;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private Button cameraCaptureButton;
    private Button galleryButton;

    // Gallery picker launcher
    private final ActivityResultLauncher<String> galleryPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    navigateToPhotoPreview(uri);
                }
            });

    // Permission request launcher
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                try {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                } catch (Exception e) {
                    Log.e(TAG, "Use case binding failed", e);
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    allGranted = allGranted && isGranted;
                }
                if (allGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Permissions are required for the camera", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    private String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        return permissions.toArray(new String[0]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        viewFinder = findViewById(R.id.viewFinder);
        cameraCaptureButton = findViewById(R.id.btnCapture);
        galleryButton = findViewById(R.id.btnGallery);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(getRequiredPermissions());
        }

        cameraCaptureButton.setOnClickListener(v -> {
            Log.d(TAG, "Capture button clicked");
            cameraCaptureButton.setEnabled(false);
            takePhoto();
        });

        // Set up gallery button
        galleryButton.setOnClickListener(v ->{
            Log.d(TAG, "Gallery button clicked");
            openGallery();
        });
    }
    private void takePhoto() {
        Log.d(TAG, "takePhoto() triggered");
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture is null");
            cameraCaptureButton.setEnabled(true);
            return;
        }

        String timestamp = new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "YoloRoad");
        }

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = output.getSavedUri();
                        if (savedUri != null) {
                            Log.d(TAG, "Photo saved: " + savedUri);
                            verifyAndStartPreview(savedUri);
                        } else {
                            Log.e(TAG, "Error: Saved URI is null");
                            showError("Failed to save image");
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        showError("Failed to capture photo: " + exception.getMessage());
                    }
                });
    }

    private void verifyAndStartPreview(Uri savedUri) {
        try {
            // Verify file access
            try (InputStream inputStream = getContentResolver().openInputStream(savedUri)) {
                if (inputStream == null) {
                    throw new Exception("Cannot access image file");
                }
            }

            // Grant permissions
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            try {
                getContentResolver().takePersistableUriPermission(savedUri, flags);
            } catch (SecurityException e) {
                Log.w(TAG, "Failed to take persistable permission", e);
            }

            // Start preview activity
            Intent intent = new Intent(this, PhotoPreviewActivity.class);
            intent.setData(savedUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error verifying saved image: " + e.getMessage(), e);
            showError("Error accessing saved image");
        }
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            cameraCaptureButton.setEnabled(true);
            Toast.makeText(CameraActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    // Method to open gallery
    private void openGallery() {
        try {
            // Use "image/*" to allow all image types
            galleryPickerLauncher.launch("image/*");
        } catch (Exception e) {
            Log.e(TAG, "Error opening gallery", e);
            Toast.makeText(this, "Unable to open gallery", Toast.LENGTH_SHORT).show();
        }
    }

    // Navigate to photo preview with selected image
    private void navigateToPhotoPreview(Uri imageUri) {
        Intent intent = new Intent(this, PhotoPreviewActivity.class);
        intent.setData(imageUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out);
        finish();
    }

    // ... (rest of the existing methods remain the same)

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Existing takePhoto(), startCamera(), and other methods remain the same
}

