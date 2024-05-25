package com.stringee.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;

import com.stringee.widget.R.styleable;

public class ScannerOverlayView extends View {
    private Rect rect;
    @ColorInt
    protected int color = Color.parseColor("#60000000");
    protected float widthRatio = 0.65f;
    protected float aspectRatio = 16 / 9f;
    protected int radius = 0;
    protected Paint finderMaskPaint;
    protected Paint borderPaint;
    protected float borderStrokeWidth = 0f;
    @ColorInt
    protected int borderStrokeColor = Color.parseColor("#ffffff");
    protected BorderType borderType = BorderType.ROUND;
    protected float dashLength = 0f;

    public enum BorderType {
        ROUND, OVAL, CIRCLE,
    }

    public ScannerOverlayView(Context context) {
        super(context);
        init();
    }

    public ScannerOverlayView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attributeSet, styleable.ScannerOverlayView, 0, 0);

        try {
            color = typedArray.getColor(styleable.ScannerOverlayView_color, color);
            widthRatio = typedArray.getFloat(styleable.ScannerOverlayView_widthRatio, widthRatio);
            aspectRatio = typedArray.getFloat(styleable.ScannerOverlayView_aspectRatio, aspectRatio);
            radius = typedArray.getInteger(styleable.ScannerOverlayView_radius, radius);
            borderStrokeWidth = typedArray.getFloat(styleable.ScannerOverlayView_borderStrokeWidth, borderStrokeWidth);
            borderStrokeColor = typedArray.getColor(styleable.ScannerOverlayView_borderStrokeColor, borderStrokeColor);
            borderType = BorderType.values()[typedArray.getInt(styleable.ScannerOverlayView_borderType, 0)];
            dashLength = typedArray.getFloat(styleable.ScannerOverlayView_dashLength, dashLength);
        } finally {
            typedArray.recycle();
        }

        init();
    }

    private void init() {
        //finder mask paint
        finderMaskPaint = new Paint();
        finderMaskPaint.setColor(color);

        //border paint
        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        if (borderStrokeWidth > 0) {
            borderPaint.setColor(borderStrokeColor);
            borderPaint.setStrokeWidth(borderStrokeWidth);
        }
        borderPaint.setStrokeJoin(Paint.Join.ROUND);
        if (radius >= 0 && dashLength <= 0) {
            borderPaint.setPathEffect(new CornerPathEffect(radius));
        }
        borderPaint.setAntiAlias(true);
        if (dashLength > 0) {
            borderPaint.setPathEffect(new DashPathEffect(new float[]{dashLength, dashLength}, 0f));
        }
    }

    @Override
    public void setId(int id) {
        super.setId(id);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (rect == null) {
            return;
        }

        drawViewFinderMask(canvas);
        drawViewFinderBorder(canvas);
    }

    private void drawViewFinderMask(Canvas canvas) {
        RectF rectF = new RectF(rect.left, rect.top, rect.right, rect.bottom);
        final Path path = new Path();
        switch (borderType) {
            case OVAL:
                path.addOval(rectF, Path.Direction.CW);
                break;
            case CIRCLE:
                path.addCircle(getWidth() / 2f, getHeight() / 2f, rect.width() / 2f, Path.Direction.CW);
                break;
            case ROUND:
            default:
                path.addRoundRect(rectF, dashLength > 0 ? 0 : Math.max(radius, 0), dashLength > 0 ? 0 : Math.max(radius, 0), Path.Direction.CW);
                break;
        }
        path.setFillType(Path.FillType.INVERSE_WINDING);
        canvas.drawPath(path, finderMaskPaint);
    }


    private void drawViewFinderBorder(Canvas canvas) {
        if (borderStrokeWidth > 0) {
            RectF rectF = new RectF(rect.left, rect.top, rect.right, rect.bottom);
            switch (borderType) {
                case OVAL:
                    canvas.drawOval(rectF, borderPaint);
                    break;
                case CIRCLE:
                    canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, rect.width() / 2f, borderPaint);
                    break;
                case ROUND:
                default:
                    canvas.drawRoundRect(rectF, radius, 0f, borderPaint);
                    break;
            }
        }
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        Point viewResolution = new Point(getWidth(), getHeight());
        int width = (int) (getWidth() * widthRatio);
        int height = (int) (width * aspectRatio);
        if (height > getHeight()) {
            height = getHeight();
        }

        int leftOffset = (viewResolution.x - width) / 2;
        int topOffset = (viewResolution.y - height) / 2;
        rect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
    }

    public ScannerOverlayView setColor(@ColorInt int color) {
        this.color = color;
        this.finderMaskPaint.setColor(color);
        invalidate();
        requestLayout();
        return this;
    }

    public ScannerOverlayView setRadius(int radius) {
        if (radius <= 0) {
            radius = 0;
        }
        this.radius = radius;
        invalidate();
        requestLayout();
        return this;
    }

    public ScannerOverlayView setWidthRatio(float widthRatio) {
        if (widthRatio > 1) {
            widthRatio = 1;
        }
        this.widthRatio = widthRatio;
        invalidate();
        requestLayout();
        return this;
    }

    public ScannerOverlayView setBorderStrokeWidth(float borderStrokeWidth) {
        if (borderStrokeWidth <= 0) {
            borderStrokeWidth = 0f;
        }
        if (borderStrokeWidth > 0) {
            borderPaint.setColor(borderStrokeColor);
            borderPaint.setStrokeWidth(borderStrokeWidth);
        }
        this.borderStrokeWidth = borderStrokeWidth;
        invalidate();
        requestLayout();
        return this;
    }

    public ScannerOverlayView setBorderStrokeColor(@ColorInt int borderStrokeColor) {
        if (borderStrokeWidth > 0) {
            borderPaint.setColor(borderStrokeColor);
            borderPaint.setStrokeWidth(borderStrokeWidth);
        }
        this.borderStrokeColor = borderStrokeColor;
        invalidate();
        requestLayout();
        return this;
    }

    public ScannerOverlayView setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        invalidate();
        requestLayout();
        return this;
    }

    public ScannerOverlayView setBorderType(BorderType borderType) {
        this.borderType = borderType;
        invalidate();
        requestLayout();
        return this;
    }

    public ScannerOverlayView setDashLength(float dashLength) {
        if (dashLength < 0) {
            dashLength = 0;
        }
        this.dashLength = dashLength;
        if (dashLength > 0) {
            borderPaint.setPathEffect(new DashPathEffect(new float[]{dashLength, dashLength}, 0f));
        }
        invalidate();
        requestLayout();
        return this;
    }
}