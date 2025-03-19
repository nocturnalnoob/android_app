package com.example.yolo_road;

import android.content.Context;
import android.util.Log;
import okhttp3.*;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;

public class ApiService {
    private static final String TAG = "ApiService";
    public static final String SERVER_URL = "http://192.168.0.103:5000";  // Update with your server IP
    private final OkHttpClient client = new OkHttpClient();

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onFailure(String error);
    }

    // Existing upload method - keeping it unchanged
    public void uploadImage(Context context, File imageFile, ApiCallback callback) {
        try {
            Log.d(TAG, "Preparing to upload image to: " + SERVER_URL);

            // Create RequestBody from file
            RequestBody requestFile = RequestBody.create(
                    MediaType.parse("image/jpeg"),
                    imageFile
            );

            // Create MultipartBody
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                            "image",  // This name should match what your Flask server expects
                            "image.jpg",
                            requestFile
                    );

            // Create request
            Request request = new Request.Builder()
                    .url(SERVER_URL + "/detect")
                    .post(builder.build())
                    .build();

            // Execute request
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network request failed", e);
                    callback.onFailure("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseData = response.body().string();
                            Log.d(TAG, "Server response: " + responseData);
                            JSONObject jsonResponse = new JSONObject(responseData);
                            callback.onSuccess(jsonResponse);
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "No error body";
                            Log.e(TAG, "Server error: " + response.code() + ", " + errorBody);
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
        } catch (Exception e) {
            Log.e(TAG, "Error preparing upload", e);
            callback.onFailure("Error preparing upload: " + e.getMessage());
        }
    }

    // New method to get all detection results
    public void getAllResults(ApiCallback callback) {
        Request request = new Request.Builder()
                .url(SERVER_URL + "/get_results")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get results", e);
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        Log.d(TAG, "Server response: " + responseData);
                        JSONObject jsonResponse = new JSONObject(responseData);
                        callback.onSuccess(jsonResponse);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        Log.e(TAG, "Server error: " + response.code() + ", " + errorBody);
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

    // Method to get a specific image
    public void getImage(String timestamp, String filename, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(SERVER_URL + "/get_image/" + timestamp + "/" + filename)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get image", e);
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        callback.onSuccess(jsonResponse);
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

    // Method to get JSON result for a specific detection
    public void getJsonResult(String timestamp, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(SERVER_URL + "/get_json/" + timestamp + "/detection.json")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get JSON result", e);
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        callback.onSuccess(jsonResponse);
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
}