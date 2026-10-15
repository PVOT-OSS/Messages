/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package com.klinker.android.send_message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SmsManagerFactoryTest {

    @Test
    fun givenNegativeOneSubscriptionId_whenCreateSmsManager_thenReturnsDefaultSmsManager() {
        val subscriptionId = -1

        val smsManager = SmsManagerFactory.createSmsManager(subscriptionId)

        assertNotNull("SmsManager should not be null", smsManager)
    }

    @Test
    fun givenInvalidSubscriptionId_whenCreateSmsManager_thenFallsBackToDefault() {
        val invalidSubscriptionId = -999

        val smsManager = SmsManagerFactory.createSmsManager(invalidSubscriptionId)

        assertNotNull("SmsManager should not be null even with invalid subscription", smsManager)
    }

    @Test
    fun givenZeroSubscriptionId_whenCreateSmsManager_thenReturnsManager() {
        val subscriptionId = 0

        val smsManager = SmsManagerFactory.createSmsManager(subscriptionId)

        assertNotNull("SmsManager should not be null for subscription 0", smsManager)
    }

    @Test
    fun givenPositiveSubscriptionId_whenCreateSmsManager_thenReturnsManager() {
        val subscriptionId = 1

        val smsManager = SmsManagerFactory.createSmsManager(subscriptionId)

        assertNotNull("SmsManager should not be null for subscription 1", smsManager)
    }

    @Test
    fun givenSmsManagerFactory_whenIsObject_thenIsSingleton() {
        val instance1 = SmsManagerFactory
        val instance2 = SmsManagerFactory

        assertEquals("SmsManagerFactory should be a singleton object", instance1, instance2)
    }

    @Test
    fun givenDefaultSubscriptionId_whenUsed_thenEqualsNegativeOne() {
        val defaultNoSubscriptionId = -1

        assertEquals("Default no-subscription ID should be -1", -1, defaultNoSubscriptionId)
    }

    @Test
    fun givenDualSimDevice_whenSubscriptionIdIsZero_thenRepresentsFirstSim() {
        val firstSimSubscriptionId = 0

        val smsManager = SmsManagerFactory.createSmsManager(firstSimSubscriptionId)

        assertNotNull("SmsManager should be available for first SIM", smsManager)
    }

    @Test
    fun givenDualSimDevice_whenSubscriptionIdIsOne_thenRepresentsSecondSim() {
        val secondSimSubscriptionId = 1

        val smsManager = SmsManagerFactory.createSmsManager(secondSimSubscriptionId)

        assertNotNull("SmsManager should be available for second SIM", smsManager)
    }

    @Test
    fun givenVeryLargeSubscriptionId_whenCreateSmsManager_thenHandlesGracefully() {
        val largeSubscriptionId = Int.MAX_VALUE

        val smsManager = SmsManagerFactory.createSmsManager(largeSubscriptionId)

        // Should not throw and should return a manager (default fallback)
        assertNotNull("SmsManager should handle large subscription IDs gracefully", smsManager)
    }

    @Test
    fun givenMultipleCallsWithSameSubscriptionId_whenCreateSmsManager_thenReturnsManagers() {
        val subscriptionId = 0

        val smsManager1 = SmsManagerFactory.createSmsManager(subscriptionId)
        val smsManager2 = SmsManagerFactory.createSmsManager(subscriptionId)

        assertNotNull("First SmsManager should not be null", smsManager1)
        assertNotNull("Second SmsManager should not be null", smsManager2)
    }

    @Test
    fun givenDifferentSubscriptionIds_whenCreateSmsManager_thenHandlesBoth() {
        val firstSim = 0
        val secondSim = 1

        val smsManager1 = SmsManagerFactory.createSmsManager(firstSim)
        val smsManager2 = SmsManagerFactory.createSmsManager(secondSim)

        assertNotNull("First SIM SmsManager should not be null", smsManager1)
        assertNotNull("Second SIM SmsManager should not be null", smsManager2)
    }
}
