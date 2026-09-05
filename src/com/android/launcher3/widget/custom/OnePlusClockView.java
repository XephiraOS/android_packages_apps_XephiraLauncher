/*
 * Copyright (C) 2026 XephiraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.widget.custom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.AlarmClock;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.R;
import com.android.launcher3.util.Executors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Native OnePlus-style Liquid Glass Clock View for XephiraOS.
 * Features signature Crimson Red accent on '1' numerals, live battery telemetry,
 * hardware RenderEffect blur, and tactile spring feedback.
 */
public class OnePlusClockView extends LinearLayout {

    private static final int CRIMSON_RED = 0xFFE60026;

    private TextView mTimeDisplay;
    private TextView mDateDisplay;
    private TextView mBatteryDisplay;

    private boolean mAttached = false;
    private int mBatteryLevel = -1;

    private final BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    mBatteryLevel = (int) ((level / (float) scale) * 100);
                }
            }
            updateClock();
        }
    };

    public OnePlusClockView(Context context) {
        this(context, null);
    }

    public OnePlusClockView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OnePlusClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClipToOutline(true);

        // Hardware RenderEffect blur on Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                setRenderEffect(RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP));
            } catch (Throwable ignored) {}
        }

        // Tap opens Clock / Alarms
        setOnClickListener(v -> {
            Intent clockIntent = new Intent(AlarmClock.ACTION_SHOW_ALARMS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                getContext().startActivity(clockIntent);
            } catch (Exception e) {
                Intent fallback = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_APP_CLOCK)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    getContext().startActivity(fallback);
                } catch (Exception ignored) {}
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTimeDisplay = findViewById(R.id.clock_time_display);
        mDateDisplay = findViewById(R.id.clock_date_display);
        mBatteryDisplay = findViewById(R.id.clock_battery_display);
        updateClock();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            getContext().registerReceiver(mTimeReceiver, filter);
            updateClock();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mTimeReceiver);
            mAttached = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                animate().scaleX(0.97f).scaleY(0.97f).setDuration(120).start();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animate().scaleX(1.0f).scaleY(1.0f).setDuration(240).start();
                break;
        }
        return super.onTouchEvent(event);
    }

    private void updateClock() {
        Calendar cal = Calendar.getInstance();
        boolean is24Hour = DateFormat.is24HourFormat(getContext());

        // Format Time
        String hourStr = new SimpleDateFormat(is24Hour ? "HH" : "h", Locale.getDefault()).format(cal.getTime());
        String minStr = new SimpleDateFormat("mm", Locale.getDefault()).format(cal.getTime());
        String fullTimeStr = hourStr + ":" + minStr;

        // Apply OnePlus signature red accent on '1' numerals in the hour
        SpannableStringBuilder ssb = new SpannableStringBuilder(fullTimeStr);
        for (int i = 0; i < hourStr.length(); i++) {
            if (hourStr.charAt(i) == '1') {
                ssb.setSpan(new ForegroundColorSpan(CRIMSON_RED), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        if (mTimeDisplay != null) {
            mTimeDisplay.setText(ssb);
        }

        // Format Date
        if (mDateDisplay != null) {
            SimpleDateFormat df = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
            mDateDisplay.setText(df.format(cal.getTime()).toUpperCase(Locale.getDefault()));
        }

        // Format Battery
        if (mBatteryDisplay != null) {
            if (mBatteryLevel >= 0) {
                mBatteryDisplay.setVisibility(View.VISIBLE);
                mBatteryDisplay.setText(mBatteryLevel + "%");
            } else {
                mBatteryDisplay.setVisibility(View.GONE);
            }
        }
    }
}
