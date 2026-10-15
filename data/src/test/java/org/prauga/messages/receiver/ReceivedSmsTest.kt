/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceivedSmsTest {

    private fun part(address: String? = "+15551234567", body: String? = "hi", time: Long = 1) =
        ReceivedSms.Part(address, body, time)

    @Test
    fun givenMultipartMessage_whenFrom_thenJoinsTheBodiesInOrder() {
        val sms = ReceivedSms.from(listOf(part(body = "Hello "), part(body = "world", time = 2)))

        assertEquals(ReceivedSms("+15551234567", "Hello world", 1), sms)
    }

    @Test
    fun givenPartsWithoutText_whenFrom_thenKeepsAnEmptyMessage() {
        val sms = ReceivedSms.from(listOf(part(body = null), part(body = null)))

        assertEquals("", sms?.body)
    }

    @Test
    fun givenSomePartsWithoutText_whenFrom_thenSkipsOnlyThose() {
        assertEquals("ab", ReceivedSms.from(listOf(part(body = "a"), part(body = null), part(body = "b")))?.body)
    }

    @Test
    fun givenNoSender_whenFrom_thenUsesUnknownSender() {
        assertEquals(ReceivedSms.UNKNOWN_SENDER, ReceivedSms.from(listOf(part(address = null)))?.address)
        assertEquals(ReceivedSms.UNKNOWN_SENDER, ReceivedSms.from(listOf(part(address = " ")))?.address)
    }

    @Test
    fun givenSenderOnlyOnALaterPart_whenFrom_thenUsesIt() {
        val sms = ReceivedSms.from(listOf(part(address = null), part(address = "+15550001111")))

        assertEquals("+15550001111", sms?.address)
    }

    @Test
    fun givenNoParts_whenFrom_thenNothingToStore() {
        assertNull(ReceivedSms.from(emptyList()))
    }
}
