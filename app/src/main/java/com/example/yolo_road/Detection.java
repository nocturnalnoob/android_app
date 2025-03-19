package com.example.yolo_road;

public class Detection {
    private String className;
    private double confidence;
    private String location;

    public Detection(String className, double confidence) {
        this.className = className;
        this.confidence = confidence;
        this.location = ""; // Default empty location
    }

    public Detection(String className, double confidence, String location) {
        this.className = className;
        this.confidence = confidence;
        this.location = location != null ? location : "";
    }

    public String getClassName() {
        return className;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getLocation() {
        return location;
    }
}