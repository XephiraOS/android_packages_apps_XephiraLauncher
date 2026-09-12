/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.launcher3.util.override;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public interface ResourceBasedOverride {

    interface Overridable {
    }

    @SuppressWarnings("unchecked")
    static <T extends ResourceBasedOverride> T newInstance(Class<T> clazz, Context context, int resId) {
        String className = null;
        try {
            className = context.getString(resId);
        } catch (Exception ignored) {
        }
        if (!TextUtils.isEmpty(className)) {
            try {
                return (T) Class.forName(className).getDeclaredConstructor(Context.class).newInstance(context);
            } catch (Throwable t) {
                try {
                    return (T) Class.forName(className).getDeclaredConstructor().newInstance();
                } catch (Throwable e) {
                    Log.e("ResourceBasedOverride", "Failed to load override class: " + className, e);
                }
            }
        }
        try {
            return clazz.getDeclaredConstructor(Context.class).newInstance(context);
        } catch (Throwable t) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Throwable e) {
                throw new RuntimeException("Cannot instantiate " + clazz.getName(), e);
            }
        }
    }
}
