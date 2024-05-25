package com.stringee.widget;

import android.graphics.Color;

import androidx.annotation.ColorInt;

import java.io.Serializable;

public class ScanViewConfig implements Serializable {
    @ColorInt
    protected int color = Color.parseColor("#60000000");
    protected float widthRatio = 0.65f;
    protected float aspectRatio = 16 / 9f;
    protected int radius = 0;
    protected float borderStrokeWidth = 0f;
    @ColorInt
    protected int borderStrokeColor = Color.parseColor("#ffffff");
    protected ScannerOverlayView.BorderType borderType = ScannerOverlayView.BorderType.ROUND;
    protected float dashLength = 0f;

    public ScanViewConfig() {
    }

    public int getColor() {
        return color;
    }

    public ScanViewConfig setColor(@ColorInt int color) {
        this.color = color;
        return this;
    }

    public float getWidthRatio() {
        if (widthRatio > 1f) {
            widthRatio = 1f;
        }
        return widthRatio;
    }

    public ScanViewConfig setWidthRatio(float widthRatio) {
        this.widthRatio = widthRatio;
        return this;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public ScanViewConfig setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        return this;
    }

    public int getRadius() {
        if (radius < 0) {
            radius = 0;
        }
        return radius;
    }

    public ScanViewConfig setRadius(int radius) {
        this.radius = radius;
        return this;
    }

    public float getBorderStrokeWidth() {
        if (borderStrokeWidth < 0) {
            borderStrokeWidth = 0;
        }
        return borderStrokeWidth;
    }

    public ScanViewConfig setBorderStrokeWidth(float borderStrokeWidth) {
        this.borderStrokeWidth = borderStrokeWidth;
        return this;
    }

    public int getBorderStrokeColor() {
        return borderStrokeColor;
    }

    public ScanViewConfig setBorderStrokeColor(int borderStrokeColor) {
        this.borderStrokeColor = borderStrokeColor;
        return this;
    }

    public ScannerOverlayView.BorderType getBorderType() {
        return borderType;
    }

    public ScanViewConfig setBorderType(ScannerOverlayView.BorderType borderType) {
        this.borderType = borderType;
        return this;
    }

    public float getDashLength() {
        if (dashLength < 0) {
            dashLength = 0;
        }
        return dashLength;
    }

    public ScanViewConfig setDashLength(float dashLength) {
        this.dashLength = dashLength;
        return this;
    }
}
