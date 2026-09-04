/*
 * Copyright (C) 2026 XephiraOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.folder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.R;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.views.ActivityContext;

import java.util.ArrayList;

/**
 * OnePlus / iOS-style Enlarged Resizable Folder View.
 * Renders a 2x2 grid container with direct 1-tap app launch capabilities.
 */
public class EnlargedFolderIcon extends FolderIcon {

    private boolean mIsEnlarged = false;
    private final Rect mHitRect = new Rect();

    public EnlargedFolderIcon(@NonNull Context context) {
        super(context);
    }

    public EnlargedFolderIcon(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEnlarged(boolean enlarged) {
        mIsEnlarged = enlarged;
        if (enlarged) {
            setBackgroundResource(R.drawable.bg_enlarged_folder_glass);
        } else {
            setBackground(null);
        }
        requestLayout();
        invalidate();
    }

    public boolean isEnlarged() {
        return mIsEnlarged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsEnlarged) {
            return super.onTouchEvent(event);
        }

        // Direct 1-tap app launch for visible items in enlarged mode
        if (event.getAction() == MotionEvent.ACTION_UP && getFolderInfo() != null) {
            float x = event.getX();
            float y = event.getY();
            ArrayList<WorkspaceItemInfo> contents = getFolderInfo().contents;

            if (contents != null && !contents.isEmpty()) {
                // Calculate which of the 2x2 quadrant cells was tapped
                int w = getWidth();
                int h = getHeight();
                int col = (x > w / 2f) ? 1 : 0;
                int row = (y > h / 2f) ? 1 : 0;
                int index = row * 2 + col;

                if (index < contents.size()) {
                    WorkspaceItemInfo tappedItem = contents.get(index);
                    // Launch directly with ItemClickHandler
                    ActivityContext actContext = ActivityContext.lookupContext(getContext());
                    if (actContext != null) {
                        ItemClickHandler.INSTANCE.onClick(this);
                        return true;
                    }
                }
            }
        }

        return super.onTouchEvent(event);
    }
}
