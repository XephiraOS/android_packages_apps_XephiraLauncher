/*
 * Copyright (C) 2026 XephiraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.R;

/**
 * Native hardware-accelerated Liquid Glass view container.
 * Renders real-time GPU frosted blur, continuous curvature, and a specular refraction rim.
 */
public class LiquidGlassFrameLayout extends FrameLayout {

    private final Paint mGlassFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSpecularRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBounds = new RectF();
    private float mCornerRadius = 32f;
    private boolean mBlurInitialized = false;

    public LiquidGlassFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    public LiquidGlassFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiquidGlassFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);

        // Glass fill: semi-translucent frosted glass
        int fillColor = context.getColor(R.color.xephira_glass_dock_bg_light);
        mGlassFillPaint.setStyle(Paint.Style.FILL);
        mGlassFillPaint.setColor(fillColor);

        // Specular rim: 1.2dp physical glass border
        mSpecularRimPaint.setStyle(Paint.Style.STROKE);
        mSpecularRimPaint.setStrokeWidth(context.getResources().getDisplayMetrics().density * 1.2f);

        // Continuous corner clipping
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), mCornerRadius);
            }
        });
        setClipToOutline(true);
    }

    public void setCornerRadius(float radiusPx) {
        mCornerRadius = radiusPx;
        invalidateOutline();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBounds.set(0, 0, w, h);

        if (w > 0 && h > 0) {
            // Setup Specular Refraction Gradient (Bright light reflection at top, soft at bottom)
            int borderTop = getContext().getColor(R.color.xephira_glass_dock_border_top);
            int borderBottom = getContext().getColor(R.color.xephira_glass_dock_border_bottom);
            mSpecularRimPaint.setShader(new LinearGradient(
                    0, 0, 0, h,
                    borderTop, borderBottom,
                    Shader.TileMode.CLAMP
            ));

            // Native GPU hardware blur on Android 12+ (S+)
            if (!mBlurInitialized && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    RenderEffect blurEffect = RenderEffect.createBlurEffect(
                            32f, 32f, Shader.TileMode.CLAMP
                    );
                    setRenderEffect(blurEffect);
                    mBlurInitialized = true;
                } catch (Exception ignored) {
                    // Fallback to pure frosted alpha blending if hardware blur unsupported
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 1. Draw frosted glass fill
        canvas.drawRoundRect(mBounds, mCornerRadius, mCornerRadius, mGlassFillPaint);

        // 2. Draw specular refraction rim
        float halfStroke = mSpecularRimPaint.getStrokeWidth() / 2f;
        RectF rimBounds = new RectF(
                mBounds.left + halfStroke,
                mBounds.top + halfStroke,
                mBounds.right - halfStroke,
                mBounds.bottom - halfStroke
        );
        canvas.drawRoundRect(rimBounds, mCornerRadius, mCornerRadius, mSpecularRimPaint);

        super.onDraw(canvas);
    }
}
