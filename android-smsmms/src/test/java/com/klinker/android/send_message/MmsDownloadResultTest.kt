/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.klinker.android.send_message

import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.testutil.MmsPduFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MmsDownloadResultTest {

    private fun retryFor(response: ByteArray?) =
        MmsReceivedReceiver.retryForResponse(response, true)

    @Test
    fun givenRetrievedMessage_whenRetryForResponse_thenNothingToRetry() {
        assertEquals(MmsRetry.NONE, retryFor(MmsPduFactory.createRetrieveConfBytes()))
        assertEquals(
            MmsRetry.NONE,
            retryFor(
                MmsPduFactory.createRetrieveConfBytes(
                    retrieveStatus = PduHeaders.RETRIEVE_STATUS_OK
                )
            ),
        )
    }

    @Test
    fun givenTransientRetrieveError_whenRetryForResponse_thenRetriesAutomatically() {
        val response =
            MmsPduFactory.createRetrieveConfBytes(
                retrieveStatus = PduHeaders.RETRIEVE_STATUS_ERROR_TRANSIENT_NETWORK_PROBLEM
            )

        assertEquals(MmsRetry.AUTO, retryFor(response))
    }

    @Test
    fun givenPermanentRetrieveError_whenRetryForResponse_thenNeverRetries() {
        val response =
            MmsPduFactory.createRetrieveConfBytes(
                retrieveStatus = PduHeaders.RETRIEVE_STATUS_ERROR_PERMANENT_MESSAGE_NOT_FOUND
            )

        assertEquals(MmsRetry.NEVER, retryFor(response))
    }

    @Test
    fun givenEmptyOrInvalidResponse_whenRetryForResponse_thenLeftToTheUser() {
        assertEquals(MmsRetry.MANUAL, retryFor(null))
        assertEquals(MmsRetry.MANUAL, retryFor(ByteArray(0)))
        assertEquals(MmsRetry.MANUAL, retryFor(MmsPduFactory.createSendConfBytes()))
    }
}
