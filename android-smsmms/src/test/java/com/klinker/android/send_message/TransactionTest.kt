/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.klinker.android.send_message

import com.google.android.mms.pdu_alt.PduHeaders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TransactionTest {

    // Broadcast Action Constants Tests
    @Test
    fun givenMmsErrorAction_thenHasCorrectValue() {
        assertEquals("com.klinker.android.send_message.MMS_ERROR", Transaction.MMS_ERROR)
    }

    @Test
    fun givenRefreshAction_thenHasCorrectValue() {
        assertEquals("com.klinker.android.send_message.REFRESH", Transaction.REFRESH)
    }

    @Test
    fun givenMmsProgressAction_thenHasCorrectValue() {
        assertEquals("com.klinker.android.send_message.MMS_PROGRESS", Transaction.MMS_PROGRESS)
    }

    @Test
    fun givenNotifyOfDeliveryAction_thenHasCorrectValue() {
        assertEquals(
            "com.klinker.android.send_message.NOTIFY_DELIVERY",
            Transaction.NOTIFY_OF_DELIVERY,
        )
    }

    @Test
    fun givenNotifyOfMmsAction_thenHasCorrectValue() {
        assertEquals("com.klinker.android.messaging.NEW_MMS_DOWNLOADED", Transaction.NOTIFY_OF_MMS)
    }

    // Intent Extra Constants Tests
    @Test
    fun givenExtraContentUri_thenHasCorrectValue() {
        assertEquals("content_uri", Transaction.EXTRA_CONTENT_URI)
    }

    @Test
    fun givenExtraFilePath_thenHasCorrectValue() {
        assertEquals("file_path", Transaction.EXTRA_FILE_PATH)
    }

    // Default Values Tests
    @Test
    fun givenDefaultExpiryTime_thenIsSevenDaysInSeconds() {
        val sevenDaysInSeconds = 7L * 24 * 60 * 60
        assertEquals(sevenDaysInSeconds, Transaction.DEFAULT_EXPIRY_TIME)
    }

    @Test
    fun givenDefaultPriority_thenIsNormal() {
        assertEquals(PduHeaders.PRIORITY_NORMAL, Transaction.DEFAULT_PRIORITY)
    }

    // Settings Tests
    @Test
    fun givenSettings_whenAccessed_thenIsNotNull() {
        assertNotNull(Transaction.settings)
    }

    @Test
    fun givenDefaultSettings_whenCreated_thenHasDefaults() {
        val settings = Settings()
        assertNotNull(settings)
    }

    // Action uniqueness Tests
    @Test
    fun givenAllActions_whenCompared_thenAllUnique() {
        val actions =
            listOf(
                Transaction.MMS_ERROR,
                Transaction.REFRESH,
                Transaction.MMS_PROGRESS,
                Transaction.NOTIFY_OF_DELIVERY,
                Transaction.NOTIFY_OF_MMS,
            )
        assertEquals(actions.size, actions.toSet().size)
    }

    // Expiry time calculation tests
    @Test
    fun givenDefaultExpiryTime_whenConvertedToDays_thenEquals7() {
        val days = Transaction.DEFAULT_EXPIRY_TIME / (24 * 60 * 60)
        assertEquals(7L, days)
    }

    @Test
    fun givenDefaultExpiryTime_whenConvertedToHours_thenEquals168() {
        val hours = Transaction.DEFAULT_EXPIRY_TIME / (60 * 60)
        assertEquals(168L, hours)
    }

    @Test
    fun givenDefaultExpiryTime_whenConvertedToMinutes_thenEquals10080() {
        val minutes = Transaction.DEFAULT_EXPIRY_TIME / 60
        assertEquals(10080L, minutes)
    }

    // Priority comparison tests
    @Test
    fun givenDefaultPriority_whenComparedToLow_thenIsHigher() {
        org.junit.Assert.assertTrue(Transaction.DEFAULT_PRIORITY > PduHeaders.PRIORITY_LOW)
    }

    @Test
    fun givenDefaultPriority_whenComparedToHigh_thenIsLower() {
        org.junit.Assert.assertTrue(Transaction.DEFAULT_PRIORITY < PduHeaders.PRIORITY_HIGH)
    }
}
