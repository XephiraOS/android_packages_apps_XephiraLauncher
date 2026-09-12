/*
 * Copyright (C) 2025 AxionOS Project
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
package com.android.quickstep.util

import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class AxAppLockerHelper private constructor() {

    companion object {
        private const val TAG = "AxAppLockerHelper"
        private const val AX_SANDBOX_SERVICE = "ax_sandbox"

        @Volatile
        private var instance: AxAppLockerHelper? = null

        @JvmStatic
        fun get(): AxAppLockerHelper {
            return instance ?: synchronized(this) {
                instance ?: AxAppLockerHelper().also { instance = it }
            }
        }
    }

    private val sandboxManagerRef = AtomicReference<Any?>()
    private val appLockCache = ConcurrentHashMap<String, Boolean>()
    private var isSupported: Boolean? = null

    private fun getSandboxManager(): Any? {
        if (isSupported == false) return null

        var manager = sandboxManagerRef.get()
        if (manager == null) {
            try {
                val binder: IBinder? = ServiceManager.getService(AX_SANDBOX_SERVICE)
                if (binder == null) {
                    isSupported = false
                    return null
                }
                val stubClass = Class.forName("com.android.internal.app.IAxSandboxManager\$Stub")
                val asInterfaceMethod: Method = stubClass.getMethod("asInterface", IBinder::class.java)
                manager = asInterfaceMethod.invoke(null, binder)
                if (manager != null) {
                    sandboxManagerRef.set(manager)
                    isSupported = true
                } else {
                    isSupported = false
                }
            } catch (t: Throwable) {
                isSupported = false
                return null
            }
        }
        return manager
    }

    fun isAppLocked(packageName: String): Boolean {
        if (packageName.isBlank()) return false

        val cached = appLockCache[packageName]
        if (cached != null) return cached

        val locked = isAppLockedWithoutCache(packageName)
        appLockCache[packageName] = locked
        return locked
    }

    fun isAppLockedWithoutCache(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val manager = getSandboxManager() ?: return false
        return try {
            val method = manager.javaClass.getMethod("getAppLockState", String::class.java)
            val state = method.invoke(manager, packageName)
            if (state is Number) {
                state.toInt() != 0
            } else if (state is Boolean) {
                state
            } else {
                false
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to query app lock state for $packageName", t)
            false
        }
    }
}
