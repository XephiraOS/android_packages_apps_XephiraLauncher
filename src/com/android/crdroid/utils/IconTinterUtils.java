/*
 * Copyright (C) 2024-2026 crDroid Android Project
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
package com.android.crdroid.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.launcher3.util.Themes;

public class IconTinterUtils {

    public static void tintIcons(PreferenceGroup group, Context context) {
        if (group == null || context == null) {
            return;
        }
        int tintColor = Themes.getColorAccent(context);
        tintPreferenceGroup(group, tintColor);
    }

    private static void tintPreferenceGroup(PreferenceGroup group, int tintColor) {
        final int count = group.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = group.getPreference(i);
            Drawable icon = pref.getIcon();
            if (icon != null) {
                icon.setTint(tintColor);
            }
            if (pref instanceof PreferenceGroup) {
                tintPreferenceGroup((PreferenceGroup) pref, tintColor);
            }
        }
    }
}
