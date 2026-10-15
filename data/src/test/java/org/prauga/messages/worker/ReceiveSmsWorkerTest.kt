/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.worker

import androidx.work.Data
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiveSmsWorkerTest {

    @Test
    fun givenInputDataKeyConstant_whenAccessed_thenReturnsCorrectKey() {
        assertEquals("messageId", ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID)
    }

    @Test
    fun givenInputData_whenBuildingWithMessageId_thenContainsMessageId() {
        val messageId = 12345L
        val inputData =
            Data.Builder().putLong(ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID, messageId).build()

        assertEquals(messageId, inputData.getLong(ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID, -1))
    }

    @Test
    fun givenInputData_whenMessageIdNotProvided_thenReturnsDefault() {
        val inputData = Data.Builder().build()

        assertEquals(-1L, inputData.getLong(ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID, -1))
    }

    @Test
    fun givenZeroMessageId_whenStored_thenReturnsZero() {
        val inputData =
            Data.Builder().putLong(ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID, 0L).build()

        assertEquals(0L, inputData.getLong(ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID, -1))
    }

    @Test
    fun givenLargeMessageId_whenStored_thenReturnsCorrectValue() {
        val messageId = Long.MAX_VALUE
        val inputData =
            Data.Builder().putLong(ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID, messageId).build()

        assertEquals(
            Long.MAX_VALUE,
            inputData.getLong(ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID, -1),
        )
    }

    @Test
    fun givenNegativeMessageId_whenChecked_thenIndicatesFailure() {
        val inputData =
            Data.Builder().putLong(ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID, -1L).build()

        val messageId = inputData.getLong(ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID, -1)
        assert(messageId == -1L) { "Negative message ID should indicate failure" }
    }

    @Test
    fun givenValidMessageId_whenChecked_thenIndicatesSuccess() {
        val inputData =
            Data.Builder().putLong(ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID, 100L).build()

        val messageId = inputData.getLong(ReceiveSmsWorker.INPUT_DATA_KEY_MESSAGE_ID, -1)
        assert(messageId != -1L) { "Valid message ID should not be -1" }
    }
}
