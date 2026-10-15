/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package org.prauga.messages.receiver

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.messages.receiver.SendSmsReceiver.Companion.ACTION_RETRY
import org.prauga.messages.receiver.SendSmsReceiver.Companion.shouldSend

class SendSmsReceiverRuleTest {

    @Test
    fun givenRetryFromTheNotification_whenTheMessageFailed_thenItIsSent() {
        assertTrue(shouldSend(ACTION_RETRY, failed = true, waiting = false))
    }

    @Test
    fun givenRetryFromTheNotification_whenItIsAlreadySendingAgain_thenItIsNotSentTwice() {
        assertFalse(shouldSend(ACTION_RETRY, failed = false, waiting = true))
    }

    @Test
    fun givenRetryFromTheNotification_whenItWentThroughSince_thenItIsNotSentAgain() {
        assertFalse(shouldSend(ACTION_RETRY, failed = false, waiting = false))
    }

    @Test
    fun givenTheDelayedSendAlarm_whenTheMessageIsWaiting_thenItIsSent() {
        assertTrue(shouldSend(null, failed = false, waiting = true))
    }

    @Test
    fun givenTheDelayedSendAlarm_whenTheMessageIsGone_thenNothingIsSent() {
        assertFalse(shouldSend(null, failed = false, waiting = false))
    }

    @Test
    fun givenANotificationFromBeforeTheUpdate_whenTheMessageFailed_thenItIsSent() {
        assertTrue(shouldSend(null, failed = true, waiting = false))
    }
}
