/*
 * Copyright (C) 2016-2024 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.util.crdroid;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class OmniJawsClient {
    private static final String TAG = "OmniJawsClient";

    public static final String ACTION_WEATHER_UPDATE = "org.omnirom.omnijaws.WEATHER_UPDATE";
    public static final String ACTION_WEATHER_ERROR = "org.omnirom.omnijaws.WEATHER_ERROR";
    public static final String ACTION_SETTINGS_CHANGED = "org.omnirom.omnijaws.SETTINGS_CHANGED";

    public static final String EXTRA_ERROR = "error";
    public static final int EXTRA_ERROR_DISABLED = 0;

    public static final String SERVICE_PACKAGE = "org.omnirom.omnijaws";
    public static final Uri WEATHER_URI = Uri.parse("content://org.omnirom.omnijaws.provider/weather");
    public static final Uri SETTINGS_URI = Uri.parse("content://org.omnirom.omnijaws.provider/settings");

    private static OmniJawsClient sInstance;

    private final List<OmniJawsObserver> mObservers = new ArrayList<>();
    private WeatherInfo mCachedWeatherInfo;
    private BroadcastReceiver mReceiver;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public interface OmniJawsObserver {
        void weatherUpdated();
        void weatherError(int errorReason);
        void updateSettings();
    }

    public static class DayForecast {
        public String low;
        public String high;
        public String condition;
        public int conditionCode;
        public String date;
    }

    public static class WeatherInfo {
        public String city;
        public String wind;
        public String condition;
        public int conditionCode;
        public String temp;
        public String tempUnits;
        public String humidity;
        public List<DayForecast> forecasts = new ArrayList<>();
        public long timeStamp;
    }

    public static synchronized OmniJawsClient get() {
        if (sInstance == null) {
            sInstance = new OmniJawsClient();
        }
        return sInstance;
    }

    public OmniJawsClient() {
    }

    public boolean isOmniJawsEnabled(Context context) {
        if (context == null) return false;
        try {
            ContentResolver cr = context.getContentResolver();
            Cursor c = cr.query(SETTINGS_URI, new String[]{"enabled"}, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        return c.getInt(0) != 0;
                    }
                } finally {
                    c.close();
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public void queryWeather(Context context) {
        if (context == null) return;
        try {
            ContentResolver cr = context.getContentResolver();
            Cursor c = cr.query(WEATHER_URI, null, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        WeatherInfo info = new WeatherInfo();
                        int cityIdx = c.getColumnIndex("city");
                        int windIdx = c.getColumnIndex("wind");
                        int condIdx = c.getColumnIndex("condition");
                        int condCodeIdx = c.getColumnIndex("condition_code");
                        int tempIdx = c.getColumnIndex("temperature");
                        int tempUnitsIdx = c.getColumnIndex("temperature_units");
                        int humIdx = c.getColumnIndex("humidity");
                        int tsIdx = c.getColumnIndex("timestamp");

                        if (cityIdx >= 0) info.city = c.getString(cityIdx);
                        if (windIdx >= 0) info.wind = c.getString(windIdx);
                        if (condIdx >= 0) info.condition = c.getString(condIdx);
                        if (condCodeIdx >= 0) info.conditionCode = c.getInt(condCodeIdx);
                        if (tempIdx >= 0) info.temp = c.getString(tempIdx);
                        if (tempUnitsIdx >= 0) info.tempUnits = c.getString(tempUnitsIdx);
                        if (humIdx >= 0) info.humidity = c.getString(humIdx);
                        if (tsIdx >= 0) info.timeStamp = c.getLong(tsIdx);

                        mCachedWeatherInfo = info;
                    }
                } finally {
                    c.close();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "queryWeather failed", e);
        }
    }

    public WeatherInfo getWeatherInfo() {
        return mCachedWeatherInfo;
    }

    public Drawable getWeatherConditionImage(Context context, int conditionCode) {
        if (context == null) return null;
        try {
            PackageManager pm = context.getPackageManager();
            Resources res = pm.getResourcesForApplication(SERVICE_PACKAGE);
            int resId = res.getIdentifier("weather_code_" + conditionCode, "drawable", SERVICE_PACKAGE);
            if (resId != 0) {
                return res.getDrawable(resId, context.getTheme());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public synchronized void addObserver(Context context, OmniJawsObserver observer) {
        if (observer == null) return;
        if (!mObservers.contains(observer)) {
            mObservers.add(observer);
        }
        if (mReceiver == null && context != null) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if (intent == null) return;
                    String action = intent.getAction();
                    if (ACTION_WEATHER_UPDATE.equals(action)) {
                        notifyWeatherUpdated();
                    } else if (ACTION_WEATHER_ERROR.equals(action)) {
                        int error = intent.getIntExtra(EXTRA_ERROR, 0);
                        notifyWeatherError(error);
                    } else if (ACTION_SETTINGS_CHANGED.equals(action)) {
                        notifySettingsChanged();
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_WEATHER_UPDATE);
            filter.addAction(ACTION_WEATHER_ERROR);
            filter.addAction(ACTION_SETTINGS_CHANGED);
            try {
                context.getApplicationContext().registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
            } catch (Exception e) {
                try {
                    context.getApplicationContext().registerReceiver(mReceiver, filter);
                } catch (Exception ignored) {}
            }
        }
    }

    public synchronized void removeObserver(Context context, OmniJawsObserver observer) {
        mObservers.remove(observer);
        if (mObservers.isEmpty() && mReceiver != null && context != null) {
            try {
                context.getApplicationContext().unregisterReceiver(mReceiver);
            } catch (Exception ignored) {
            }
            mReceiver = null;
        }
    }

    private void notifyWeatherUpdated() {
        mHandler.post(() -> {
            for (OmniJawsObserver o : new ArrayList<>(mObservers)) {
                o.weatherUpdated();
            }
        });
    }

    private void notifyWeatherError(int error) {
        mHandler.post(() -> {
            for (OmniJawsObserver o : new ArrayList<>(mObservers)) {
                o.weatherError(error);
            }
        });
    }

    private void notifySettingsChanged() {
        mHandler.post(() -> {
            for (OmniJawsObserver o : new ArrayList<>(mObservers)) {
                o.updateSettings();
            }
        });
    }
}
