package com.example.yolo_road;

import java.util.List;

public class ResultItem {
    private String timestamp;
    private String resultImageUrl;
    private List<String> detections;

    public ResultItem(String timestamp, String resultImageUrl, List<String> detections) {
        this.timestamp = timestamp;
        this.resultImageUrl = resultImageUrl;
        this.detections = detections;
    }

    public String getTimestamp() { return timestamp; }
    public String getResultImageUrl() { return resultImageUrl; }
    public List<String> getDetections() { return detections; }
}
