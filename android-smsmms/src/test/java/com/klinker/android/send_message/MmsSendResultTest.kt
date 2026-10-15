/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package com.klinker.android.send_message

import android.app.Activity
import android.telephony.SmsManager
import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.testutil.MmsPduFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MmsSendResultTest {

    @Test
    fun givenCarrierAcceptedMessage_whenFrom_thenSentWithItsMessageId() {
        val response = MmsPduFactory.createSendConfBytes(messageId = "abc123")

        val result = MmsSendResult.from(Activity.RESULT_OK, 200, response)

        assertEquals(MmsRetry.NONE, result.retry)
        assertEquals("abc123", result.messageId)
        assertEquals(PduHeaders.RESPONSE_STATUS_OK, result.responseStatus)
    }

    @Test
    fun givenTransientCarrierFailure_whenFrom_thenRetriesAutomatically() {
        val response =
            MmsPduFactory.createSendConfBytes(
                responseStatus = PduHeaders.RESPONSE_STATUS_ERROR_TRANSIENT_NETWORK_PROBLEM
            )

        assertEquals(MmsRetry.AUTO, MmsSendResult.from(Activity.RESULT_OK, 200, response).retry)
    }

    @Test
    fun givenPermanentCarrierFailure_whenFrom_thenNeverRetries() {
        val response =
            MmsPduFactory.createSendConfBytes(
                responseStatus = PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_SERVICE_DENIED
            )

        val result = MmsSendResult.from(Activity.RESULT_OK, 200, response)

        assertEquals(MmsRetry.NEVER, result.retry)
        assertNull(result.messageId)
    }

    @Test
    fun givenNoCarrierResponse_whenFrom_thenTrustsTheResultCodeLikeBefore() {
        assertEquals(MmsRetry.NONE, MmsSendResult.from(Activity.RESULT_OK, 0, null).retry)
        assertEquals(MmsRetry.NONE, MmsSendResult.from(Activity.RESULT_OK, 0, byteArrayOf(1, 2)).retry)
    }

    @Test
    fun givenErrorCodes_whenRetryForError_thenMatchesGoogleMessages() {
        assertEquals(MmsRetry.NEVER, MmsSendResult.retryForError(SmsManager.MMS_ERROR_HTTP_FAILURE, 404))
        assertEquals(MmsRetry.AUTO, MmsSendResult.retryForError(SmsManager.MMS_ERROR_HTTP_FAILURE, 400))
        assertEquals(MmsRetry.AUTO, MmsSendResult.retryForError(SmsManager.MMS_ERROR_IO_ERROR, 0))
        assertEquals(MmsRetry.MANUAL, MmsSendResult.retryForError(SmsManager.MMS_ERROR_NO_DATA_NETWORK, 0))
        assertEquals(MmsRetry.MANUAL, MmsSendResult.retryForError(SmsManager.MMS_ERROR_UNSPECIFIED, 0))
        assertEquals(MmsRetry.NEVER, MmsSendResult.retryForError(MmsSendResult.RESULT_TOO_LARGE, 0))
    }
}
