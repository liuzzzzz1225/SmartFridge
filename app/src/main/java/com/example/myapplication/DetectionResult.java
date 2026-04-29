package com.example.myapplication;

public class DetectionResult {
    public float left;
    public float top;
    public float right;
    public float bottom;
    public String label;
    public float confidence;

    public DetectionResult(float left, float top, float right, float bottom, String label, float confidence) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.label = label;
        this.confidence = confidence;
    }
} 