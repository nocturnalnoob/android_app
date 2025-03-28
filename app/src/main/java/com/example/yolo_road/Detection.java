package com.example.yolo_road;

import android.os.Parcel;
import android.os.Parcelable;

public class Detection implements Parcelable {
    private String className;
    private double confidence;
    private String location;
    private String imageUrl;

    public Detection(String className, double confidence, String location, String imageUrl) {
        this.className = className;
        this.confidence = confidence;
        this.location = location;
        this.imageUrl = imageUrl;
    }

    // Parcelable implementation
    protected Detection(Parcel in) {
        className = in.readString();
        confidence = in.readDouble();
        location = in.readString();
        imageUrl = in.readString();
    }

    public static final Creator<Detection> CREATOR = new Creator<Detection>() {
        @Override
        public Detection createFromParcel(Parcel in) {
            return new Detection(in);
        }

        @Override
        public Detection[] newArray(int size) {
            return new Detection[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(className);
        dest.writeDouble(confidence);
        dest.writeString(location);
        dest.writeString(imageUrl);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters
    public String getClassName() {
        return className;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getLocation() {
        return location;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}