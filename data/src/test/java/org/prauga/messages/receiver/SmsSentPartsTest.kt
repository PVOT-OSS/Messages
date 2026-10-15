/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.messages.receiver.SmsSentParts.Outcome

class SmsSentPartsTest {

    private val parts = SmsSentParts()

    @Test
    fun givenSinglePart_whenSent_thenMessageIsSent() {
        assertEquals(Outcome.SENT, parts.onResult(1, 0, part = 0, parts = 1, ok = true))
    }

    @Test
    fun givenSinglePart_whenFailed_thenMessageFails() {
        assertEquals(Outcome.FAILED, parts.onResult(1, 0, part = 0, parts = 1, ok = false))
    }

    @Test
    fun givenMultipart_whenEveryPartIsSent_thenOnlyTheLastPartSendsTheMessage() {
        assertEquals(Outcome.NONE, parts.onResult(1, 7, part = 0, parts = 3, ok = true))
        assertEquals(Outcome.NONE, parts.onResult(1, 7, part = 1, parts = 3, ok = true))
        assertEquals(Outcome.SENT, parts.onResult(1, 7, part = 2, parts = 3, ok = true))
    }

    @Test
    fun givenMultipart_whenEveryPartFails_thenOnlyTheFirstFailureNotifies() {
        assertEquals(Outcome.FAILED, parts.onResult(1, 7, part = 0, parts = 3, ok = false))
        assertEquals(Outcome.FAILED_SILENTLY, parts.onResult(1, 7, part = 1, parts = 3, ok = false))
        assertEquals(Outcome.FAILED_SILENTLY, parts.onResult(1, 7, part = 2, parts = 3, ok = false))
    }

    @Test
    fun givenMultipart_whenAMiddlePartFails_thenTheLastPartDoesNotSendTheMessage() {
        assertEquals(Outcome.NONE, parts.onResult(1, 7, part = 0, parts = 3, ok = true))
        assertEquals(Outcome.FAILED, parts.onResult(1, 7, part = 1, parts = 3, ok = false))
        assertEquals(Outcome.NONE, parts.onResult(1, 7, part = 2, parts = 3, ok = true))
    }

    @Test
    fun givenAFailedAttempt_whenTheRetrySucceeds_thenItIsSent() {
        assertEquals(Outcome.FAILED, parts.onResult(1, 7, part = 0, parts = 2, ok = false))
        assertEquals(Outcome.NONE, parts.onResult(1, 7, part = 1, parts = 2, ok = true))
        assertEquals(Outcome.NONE, parts.onResult(1, 8, part = 0, parts = 2, ok = true))
        assertEquals(Outcome.SENT, parts.onResult(1, 8, part = 1, parts = 2, ok = true))
    }

    @Test
    fun givenMultipart_whenTheLastPartReportsFirst_thenALaterFailureStillNotifies() {
        assertEquals(Outcome.SENT, parts.onResult(1, 7, part = 1, parts = 2, ok = true))
        assertEquals(Outcome.FAILED, parts.onResult(1, 7, part = 0, parts = 2, ok = false))
    }

    @Test
    fun givenAFailedAttempt_whenTheRetryFails_thenItNotifiesAgain() {
        assertEquals(Outcome.FAILED, parts.onResult(1, 7, part = 0, parts = 2, ok = false))
        assertEquals(Outcome.FAILED, parts.onResult(1, 8, part = 0, parts = 2, ok = false))
    }

    @Test
    fun givenTwoMessages_whenBothFail_thenBothNotify() {
        assertEquals(Outcome.FAILED, parts.onResult(1, 7, part = 0, parts = 2, ok = false))
        assertEquals(Outcome.FAILED, parts.onResult(2, 7, part = 0, parts = 2, ok = false))
    }
}
