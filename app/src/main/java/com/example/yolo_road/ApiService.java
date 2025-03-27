package com.example.yolo_road;

import android.content.Context;
import android.util.Log;
import android.graphics.Bitmap;
import android.util.Base64;
import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.Date;

public class ApiService {
    public static final String SUPABASE_KEY ="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InN5Y3BzaXN2bXdkenFteGtrb3B0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDI1NjY3MDIsImV4cCI6MjA1ODE0MjcwMn0.PLgAJLuruCnbnmQF476VVpnQiVWZaNIa-YJ2VSAxQkY";
    private static final String TAG = "ApiService";
    public static final String SUPABASE_URL = "https://sycpsisvmwdzqmxkkopt.supabase.co";
    private static final String STORAGE_BUCKET = "detection-images";
    private final OkHttpClient client = new OkHttpClient();

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onFailure(String error);
    }
    // Add this to your ApiService class

    // Helper method to convert file to byte array
    private byte[] fileToBytes(File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        fis.read(bytes);
        fis.close();
        return bytes;
    }

    // Upload image directly to Supabase Storage
    public void uploadImage(Context context, File imageFile, ApiCallback callback) {
        try {
            Log.d(TAG, "Preparing to upload image to Supabase Storage");

            String docId = UUID.randomUUID().toString();
            String folderPath = "uploads/" + docId;
            String fileName = imageFile.getName();
            String storagePath = folderPath + "/" + fileName;

            // First, upload to storage
            RequestBody fileBody = RequestBody.create(
                    MediaType.parse("image/jpeg"),
                    fileToBytes(imageFile)
            );

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("", fileName, fileBody)
                    .build();

            String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + STORAGE_BUCKET + "/" + storagePath;
            Log.d(TAG, "Upload URL: " + uploadUrl);

            Request storageRequest = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .build();

            client.newCall(storageRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Storage upload failed", e);
                    callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Log.d(TAG, "Storage response: " + responseBody);

                        if (response.isSuccessful()) {
                            // Get the public URL
                            String imageUrl = SUPABASE_URL + "/storage/v1/object/public/" +
                                    STORAGE_BUCKET + "/" + storagePath;

                            // Create metadata JSON object
                            JSONObject metadata = new JSONObject();
                            try {
                                metadata.put("original_name", fileName);
                                metadata.put("file_size", imageFile.length());
                                metadata.put("mime_type", "image/jpeg");
                                metadata.put("upload_path", storagePath);
                            } catch (JSONException e) {
                                Log.e(TAG, "Error creating metadata", e);
                            }

                            // Create database entry
                            JSONObject document = new JSONObject();
                            try {
                                document.put("id", docId);
                                document.put("image_url", imageUrl);
                                document.put("status", "pending");
                                document.put("filename", fileName);
                                document.put("image_path", storagePath);
                                document.put("uploaded_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
                                        .format(new Date()));
                                document.put("metadata", metadata);

                                // Optional: Add image_data if needed
                                // document.put("image_data", Base64.encodeToString(fileToBytes(imageFile), Base64.DEFAULT));
                            } catch (JSONException e) {
                                Log.e(TAG, "Error creating document JSON", e);
                                callback.onFailure("Error creating document: " + e.getMessage());
                                return;
                            }

                            Log.d(TAG, "Sending to database: " + document.toString());

                            // Create database request
                            Request dbRequest = new Request.Builder()
                                    .url(SUPABASE_URL + "/rest/v1/pending_images")
                                    .post(RequestBody.create(
                                            MediaType.parse("application/json"),
                                            document.toString()
                                    ))
                                    .addHeader("apikey", SUPABASE_KEY)
                                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Prefer", "return=minimal")
                                    .build();

                            // Execute database request
                            client.newCall(dbRequest).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    Log.e(TAG, "Database insert failed", e);
                                    callback.onFailure("Database error: " + e.getMessage());
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    String dbResponseBody = response.body() != null ? response.body().string() : "";
                                    Log.d(TAG, "Database response code: " + response.code());
                                    Log.d(TAG, "Database response: " + dbResponseBody);

                                    if (response.isSuccessful()) {
                                        try {
                                            JSONObject successResponse = new JSONObject();
                                            successResponse.put("success", true);
                                            successResponse.put("id", docId);
                                            successResponse.put("image_url", imageUrl);
                                            callback.onSuccess(successResponse);
                                        } catch (JSONException e) {
                                            Log.e(TAG, "Error creating success response", e);
                                            callback.onFailure("Error creating success response: " + e.getMessage());
                                        }
                                    } else {
                                        callback.onFailure("Database error " + response.code() + ": " + dbResponseBody);
                                    }
                                    response.close();
                                }
                            });
                        } else {
                            callback.onFailure("Storage error " + response.code() + ": " + responseBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing storage response", e);
                        callback.onFailure("Error processing storage response: " + e.getMessage());
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing upload", e);
            callback.onFailure("Error preparing upload: " + e.getMessage());
        }
    }

    // Helper method to convert file to bytes

    // Get all detection results from Supabase
    public void getAllResults(ApiCallback callback) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/detections?select=*&order=created_at.desc")
                .get()
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get results from Supabase", e);
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    Log.d(TAG, "Raw response: " + responseData);

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put("results", new JSONArray(responseData));
                        callback.onSuccess(jsonResponse);
                    } else {
                        Log.e(TAG, "Error response: " + responseData);
                        callback.onFailure("Server error " + response.code() + ": " + responseData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    callback.onFailure("Error processing response: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    // Check processing status
    public void checkStatus(String imageId, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/pending_images?id=eq." + imageId)
                .get()
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to check status", e);
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        if (jsonArray.length() > 0) {
                            JSONObject statusObj = jsonArray.getJSONObject(0);
                            callback.onSuccess(statusObj);
                        } else {
                            callback.onFailure("Image not found");
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        callback.onFailure("Server error " + response.code() + ": " + errorBody);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    callback.onFailure("Error processing response: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }
    // Add this method to ApiService class
    public void getResultsPage(int page, int pageSize, ApiCallback callback) {
        String url = SUPABASE_URL + "/rest/v1/detections"
                + "?select=*"
                + "&order=created_at.desc"
                + "&limit=" + pageSize
                + "&offset=" + (page * pageSize);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get results page", e);
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseData = response.body().string();
                    Log.d(TAG, "Page " + page + " response: " + responseData);

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put("results", new JSONArray(responseData));
                        callback.onSuccess(jsonResponse);
                    } else {
                        Log.e(TAG, "Error response: " + responseData);
                        callback.onFailure("Server error " + response.code() + ": " + responseData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    callback.onFailure("Error processing response: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }
}