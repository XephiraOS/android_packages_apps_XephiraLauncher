/*
 * Copyright (C) 2026 XephiraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.util;

import android.view.animation.Interpolator;

/**
 * High-precision Damped Harmonic Oscillator (Spring) Interpolator.
 * Models physical spring dynamics: F = -kx - cv
 *
 * Provides organic, critically-damped momentum for 120Hz/144Hz app launch and close transitions.
 */
public class LiquidSpringInterpolator implements Interpolator {

    /** Default spring for app open: fluid, organic expansion */
    public static final LiquidSpringInterpolator APP_OPEN_SPRING =
            new LiquidSpringInterpolator(0.86f, 24.0f);

    /** Spring for app close & swipe-up return: responsive, confident snap */
    public static final LiquidSpringInterpolator APP_CLOSE_SPRING =
            new LiquidSpringInterpolator(0.90f, 26.0f);

    /** Spring for icon squish on press: energetic tactile rebound */
    public static final LiquidSpringInterpolator ICON_SQUISH_SPRING =
            new LiquidSpringInterpolator(0.80f, 30.0f);

    private final float mDampingRatio; // zeta
    private final float mNaturalFrequency; // omega_0 (rad/s)
    private final float mDampedFrequency; // omega_d

    /**
     * @param dampingRatio Damping ratio zeta (0.80 to 0.95 for organic critical damping)
     * @param naturalFrequency Natural frequency in rad/s (higher = snappier)
     */
    public LiquidSpringInterpolator(float dampingRatio, float naturalFrequency) {
        mDampingRatio = dampingRatio;
        mNaturalFrequency = naturalFrequency;
        if (dampingRatio < 1.0f) {
            mDampedFrequency = (float) (naturalFrequency * Math.sqrt(1.0f - dampingRatio * dampingRatio));
        } else {
            mDampedFrequency = 0f;
        }
    }

    @Override
    public float getInterpolation(float input) {
        if (input <= 0f) return 0f;
        if (input >= 1f) return 1f;

        // Physical spring displacement: x(t) = 1 - e^(-zeta * omega_0 * t) * (cos(omega_d * t) + (zeta*omega_0/omega_d)*sin(omega_d * t))
        float t = input * 0.35f; // Scale normalized input [0, 1] to physical time (~350ms window)
        float decay = (float) Math.exp(-mDampingRatio * mNaturalFrequency * t);

        if (mDampingRatio < 1.0f) {
            float cos = (float) Math.cos(mDampedFrequency * t);
            float sin = (float) Math.sin(mDampedFrequency * t);
            float envelope = cos + (mDampingRatio * mNaturalFrequency / mDampedFrequency) * sin;
            float val = 1.0f - decay * envelope;

            // Normalize endpoint so val reaches exactly 1.0 at input = 1.0
            return Math.min(1.0f, Math.max(0.0f, val));
        } else {
            // Overdamped / critically damped
            float val = 1.0f - decay * (1.0f + mNaturalFrequency * t);
            return Math.min(1.0f, Math.max(0.0f, val));
        }
    }
}
