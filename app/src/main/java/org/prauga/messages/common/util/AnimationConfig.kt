/*
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.common.util

/**
 * Configuration object containing animation duration constants.
 * 
 * These durations are used throughout the app to maintain consistent
 * animation timing and feel. All durations are in milliseconds.
 */
object AnimationConfig {
    /**
     * Standard fade in/out duration for UI elements.
     * 
     * Used for: DetailedChipView show/hide animations
     * Duration: 200ms provides smooth but quick transitions
     */
    const val FADE_DURATION_MS = 200L
    
    /**
     * Duration for detailed chip view animations.
     * 
     * Used for: Chip expansion and collapse animations
     * Duration: 200ms matches the fade duration for consistency
     */
    const val CHIP_ANIMATION_DURATION_MS = 200L
    
    /**
     * Oval animation duration for mic input cloud (first oval).
     * 
     * Used for: MicInputCloudView oval one rotation animation
     * Duration: 2000ms creates a slow, smooth rotation effect
     */
    const val OVAL_ONE_ANIMATION_LENGTH_MS = 2000L
    
    /**
     * Oval animation duration for mic input cloud (second oval).
     * 
     * Used for: MicInputCloudView oval two rotation animation
     * Duration: 1000ms creates a faster rotation than oval one
     */
    const val OVAL_TWO_ANIMATION_LENGTH_MS = 1000L
}
