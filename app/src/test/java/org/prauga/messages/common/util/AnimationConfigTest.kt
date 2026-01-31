/*
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AnimationConfig.
 * 
 * These tests verify that all animation duration constants are defined
 * with positive values and maintain expected relationships.
 */
class AnimationConfigTest {

    @Test
    fun givenFadeDuration_whenAccessed_thenIsPositive() {
        // When
        val duration = AnimationConfig.FADE_DURATION_MS

        // Then
        assertTrue("Fade duration should be positive", duration > 0)
    }

    @Test
    fun givenChipAnimationDuration_whenAccessed_thenIsPositive() {
        // When
        val duration = AnimationConfig.CHIP_ANIMATION_DURATION_MS

        // Then
        assertTrue("Chip animation duration should be positive", duration > 0)
    }

    @Test
    fun givenOvalOneAnimationLength_whenAccessed_thenIsPositive() {
        // When
        val duration = AnimationConfig.OVAL_ONE_ANIMATION_LENGTH_MS

        // Then
        assertTrue("Oval one animation length should be positive", duration > 0)
    }

    @Test
    fun givenOvalTwoAnimationLength_whenAccessed_thenIsPositive() {
        // When
        val duration = AnimationConfig.OVAL_TWO_ANIMATION_LENGTH_MS

        // Then
        assertTrue("Oval two animation length should be positive", duration > 0)
    }

    @Test
    fun givenFadeAndChipDurations_whenCompared_thenAreEqual() {
        // When
        val fadeDuration = AnimationConfig.FADE_DURATION_MS
        val chipDuration = AnimationConfig.CHIP_ANIMATION_DURATION_MS

        // Then
        assertEquals("Fade and chip durations should match for consistency", 
            fadeDuration, chipDuration)
    }

    @Test
    fun givenOvalAnimations_whenCompared_thenOvalOneIsSlower() {
        // When
        val ovalOneDuration = AnimationConfig.OVAL_ONE_ANIMATION_LENGTH_MS
        val ovalTwoDuration = AnimationConfig.OVAL_TWO_ANIMATION_LENGTH_MS

        // Then
        assertTrue("Oval one should be slower (longer duration) than oval two",
            ovalOneDuration > ovalTwoDuration)
    }

    @Test
    fun givenAllDurations_whenAccessed_thenAreReasonable() {
        // When
        val fadeDuration = AnimationConfig.FADE_DURATION_MS
        val chipDuration = AnimationConfig.CHIP_ANIMATION_DURATION_MS
        val ovalOneDuration = AnimationConfig.OVAL_ONE_ANIMATION_LENGTH_MS
        val ovalTwoDuration = AnimationConfig.OVAL_TWO_ANIMATION_LENGTH_MS

        // Then - animations should be between 50ms and 5000ms for good UX
        assertTrue("Fade duration should be reasonable (50-5000ms)", 
            fadeDuration in 50..5000)
        assertTrue("Chip duration should be reasonable (50-5000ms)",
            chipDuration in 50..5000)
        assertTrue("Oval one duration should be reasonable (50-5000ms)",
            ovalOneDuration in 50..5000)
        assertTrue("Oval two duration should be reasonable (50-5000ms)",
            ovalTwoDuration in 50..5000)
    }

    @Test
    fun givenFadeDuration_whenAccessed_thenHasExpectedValue() {
        // When
        val duration = AnimationConfig.FADE_DURATION_MS

        // Then
        assertEquals("Fade duration should be 200ms", 200L, duration)
    }

    @Test
    fun givenChipAnimationDuration_whenAccessed_thenHasExpectedValue() {
        // When
        val duration = AnimationConfig.CHIP_ANIMATION_DURATION_MS

        // Then
        assertEquals("Chip animation duration should be 200ms", 200L, duration)
    }

    @Test
    fun givenOvalOneAnimationLength_whenAccessed_thenHasExpectedValue() {
        // When
        val duration = AnimationConfig.OVAL_ONE_ANIMATION_LENGTH_MS

        // Then
        assertEquals("Oval one animation length should be 2000ms", 2000L, duration)
    }

    @Test
    fun givenOvalTwoAnimationLength_whenAccessed_thenHasExpectedValue() {
        // When
        val duration = AnimationConfig.OVAL_TWO_ANIMATION_LENGTH_MS

        // Then
        assertEquals("Oval two animation length should be 1000ms", 1000L, duration)
    }
}
