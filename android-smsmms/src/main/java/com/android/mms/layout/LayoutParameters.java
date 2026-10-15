/*
 * SPDX-FileCopyrightText: 2008 Esmertec AG.
 * SPDX-FileCopyrightText: 2008 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.mms.layout;

public interface LayoutParameters {
    /* Layouts type definition */
    public static final int UNKNOWN        = -1;
    public static final int HVGA_LANDSCAPE = 10;
    public static final int HVGA_PORTRAIT  = 11;

    /* Parameters for known layouts */
    public static final int HVGA_LANDSCAPE_WIDTH  = 480;
    public static final int HVGA_LANDSCAPE_HEIGHT = 320;
    public static final int HVGA_PORTRAIT_WIDTH   = 320;
    public static final int HVGA_PORTRAIT_HEIGHT  = 480;

    /**
     * Get the width of current layout.
     */
    int getWidth();
    /**
     * Get the height of current layout.
     */
    int getHeight();
    /**
     * Get the width of the image region of current layout.
     */
    int getImageHeight();
    /**
     * Get the height of the text region of current layout.
     */
    int getTextHeight();
    /**
     * Get the type of current layout.
     */
    int getType();
    /**
     * Get the type description of current layout.
     */
    String getTypeDescription();
}
