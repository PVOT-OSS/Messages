/*
 * Copyright (C) 2026 Vishnu R <vishnurajesh45@gmail.com>
 */

package org.prauga.messages.app.resources

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.app.R
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for dimension resources.
 * 
 * These tests verify that all dimension resources are properly defined
 * and have the expected values after externalization.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class DimensionResourcesTest {

    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    // Icon Size Tests
    @Test
    fun iconSizeSmall_shouldBe24dp() {
        val expected = dpToPx(24f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.icon_size_small)
        
        assertEquals("icon_size_small should be 24dp", expected, actual)
    }

    @Test
    fun iconSizeMedium_shouldBe56dp() {
        val expected = dpToPx(56f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.icon_size_medium)
        
        assertEquals("icon_size_medium should be 56dp", expected, actual)
    }

    // Padding Tests
    @Test
    fun paddingStandard_shouldBe24dp() {
        val expected = dpToPx(24f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.padding_standard)
        
        assertEquals("padding_standard should be 24dp", expected, actual)
    }

    @Test
    fun paddingSmall_shouldBe8dp() {
        val expected = dpToPx(8f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.padding_small)
        
        assertEquals("padding_small should be 8dp", expected, actual)
    }

    @Test
    fun paddingReactions_shouldBe25dp() {
        val expected = dpToPx(25f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.padding_reactions)
        
        assertEquals("padding_reactions should be 25dp", expected, actual)
    }

    // Margin Tests
    @Test
    fun marginChipTop_shouldBe24dp() {
        val expected = dpToPx(24f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.margin_chip_top)
        
        assertEquals("margin_chip_top should be 24dp", expected, actual)
    }

    @Test
    fun marginChipStart_shouldBe56dp() {
        val expected = dpToPx(56f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.margin_chip_start)
        
        assertEquals("margin_chip_start should be 56dp", expected, actual)
    }

    // Mic Cloud Radius Tests
    @Test
    fun micCloudRadiusDefault_shouldBe70dp() {
        val expected = dpToPx(70f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.mic_cloud_radius_default)
        
        assertEquals("mic_cloud_radius_default should be 70dp", expected, actual)
    }

    @Test
    fun micCloudRadiusExtended_shouldBe75dp() {
        val expected = dpToPx(75f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.mic_cloud_radius_extended)
        
        assertEquals("mic_cloud_radius_extended should be 75dp", expected, actual)
    }

    @Test
    fun micCloudRadiusMaximum_shouldBe80dp() {
        val expected = dpToPx(80f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.mic_cloud_radius_maximum)
        
        assertEquals("mic_cloud_radius_maximum should be 80dp", expected, actual)
    }

    // Bubble Radius Tests
    @Test
    fun bubbleRadiusSmall_shouldBe4dp() {
        val expected = dpToPx(4f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.bubble_radius_small)
        
        assertEquals("bubble_radius_small should be 4dp", expected, actual)
    }

    @Test
    fun bubbleRadiusLarge_shouldBe18dp() {
        val expected = dpToPx(18f)
        val actual = context.resources.getDimensionPixelSize(R.dimen.bubble_radius_large)
        
        assertEquals("bubble_radius_large should be 18dp", expected, actual)
    }

    // Validation Tests
    @Test
    fun allDimensions_shouldBePositive() {
        val dimensions = listOf(
            R.dimen.icon_size_small,
            R.dimen.icon_size_medium,
            R.dimen.padding_standard,
            R.dimen.padding_small,
            R.dimen.padding_reactions,
            R.dimen.margin_chip_top,
            R.dimen.margin_chip_start,
            R.dimen.mic_cloud_radius_default,
            R.dimen.mic_cloud_radius_extended,
            R.dimen.mic_cloud_radius_maximum,
            R.dimen.bubble_radius_small,
            R.dimen.bubble_radius_large
        )

        dimensions.forEach { dimenId ->
            val value = context.resources.getDimensionPixelSize(dimenId)
            assertTrue(
                "Dimension ${context.resources.getResourceEntryName(dimenId)} should be positive",
                value > 0
            )
        }
    }

    @Test
    fun micCloudRadii_shouldBeInAscendingOrder() {
        val defaultRadius = context.resources.getDimensionPixelSize(R.dimen.mic_cloud_radius_default)
        val extendedRadius = context.resources.getDimensionPixelSize(R.dimen.mic_cloud_radius_extended)
        val maximumRadius = context.resources.getDimensionPixelSize(R.dimen.mic_cloud_radius_maximum)

        assertTrue("default < extended", defaultRadius < extendedRadius)
        assertTrue("extended < maximum", extendedRadius < maximumRadius)
    }

    @Test
    fun bubbleRadii_shouldBeInAscendingOrder() {
        val smallRadius = context.resources.getDimensionPixelSize(R.dimen.bubble_radius_small)
        val largeRadius = context.resources.getDimensionPixelSize(R.dimen.bubble_radius_large)

        assertTrue("small < large", smallRadius < largeRadius)
    }

    // Helper function to convert dp to pixels
    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
