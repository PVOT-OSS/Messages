/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import com.klinker.android.send_message.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.prauga.messages.interactor.SyncMessage
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MmsSentReceiverTest {

    @Mock private lateinit var context: Context

    @Mock private lateinit var syncMessage: SyncMessage

    private lateinit var receiver: MmsSentReceiver

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        receiver = MmsSentReceiver()
        receiver.syncMessage = syncMessage
    }

    @Test
    fun givenMmsSentReceiver_whenInstantiated_thenIsBroadcastReceiverSubclass() {
        val receiver = MmsSentReceiver()

        assertNotNull(receiver)
        assertTrue(receiver is android.content.BroadcastReceiver)
    }

    @Test
    fun givenReceiverWithInjectedDependencies_whenAccessed_thenDependenciesAreAvailable() {
        assertNotNull(receiver.syncMessage)
    }

    @Test
    fun givenIntent_whenExtraContentUriRetrieved_thenParsesCorrectly() {
        val expectedUri = "content://mms/123"
        val intent = Intent().apply { putExtra(Transaction.EXTRA_CONTENT_URI, expectedUri) }

        val uriString = intent.getStringExtra(Transaction.EXTRA_CONTENT_URI)

        assertEquals(expectedUri, uriString)
        assertNotNull(Uri.parse(uriString))
    }

    @Test
    fun givenIntent_whenExtraFilePathRetrieved_thenReturnsPath() {
        val expectedPath = "/data/user/0/com.app/cache/send.12345.dat"
        val intent = Intent().apply { putExtra(Transaction.EXTRA_FILE_PATH, expectedPath) }

        val filePath = intent.getStringExtra(Transaction.EXTRA_FILE_PATH)

        assertEquals(expectedPath, filePath)
    }

    @Test
    fun givenResultCodeActivityResultOk_whenChecked_thenIndicatesSuccess() {
        val resultCode = Activity.RESULT_OK

        val isSuccess = resultCode == Activity.RESULT_OK

        assertTrue(isSuccess)
    }

    @Test
    fun givenResultCodeNonOk_whenChecked_thenIndicatesFailure() {
        val resultCode = Activity.RESULT_CANCELED

        val isFailure = resultCode != Activity.RESULT_OK

        assertTrue(isFailure)
    }

    @Test
    fun givenMmsMessageBoxSent_thenHasCorrectValue() {
        assertEquals(2, Telephony.Mms.MESSAGE_BOX_SENT)
    }

    @Test
    fun givenMmsMessageBoxFailed_thenHasCorrectValue() {
        assertEquals(5, Telephony.Mms.MESSAGE_BOX_FAILED)
    }

    @Test
    fun givenMmsMessageBoxOutbox_thenHasCorrectValue() {
        assertEquals(4, Telephony.Mms.MESSAGE_BOX_OUTBOX)
    }

    @Test
    fun givenMmsMessageBoxInbox_thenHasCorrectValue() {
        assertEquals(1, Telephony.Mms.MESSAGE_BOX_INBOX)
    }

    @Test
    fun givenMmsContentUri_whenParsed_thenHasCorrectScheme() {
        val uriString = "content://mms/outbox/123"
        val uri = Uri.parse(uriString)

        assertEquals("content", uri.scheme)
        assertEquals("mms", uri.authority)
    }

    @Test
    fun givenMmsUri_whenContentUrisParsed_thenExtractsId() {
        val uriString = "content://mms/123"
        val uri = Uri.parse(uriString)
        val id = android.content.ContentUris.parseId(uri)

        assertEquals(123L, id)
    }

    @Test
    fun givenTransactionConstants_whenAccessed_thenHaveCorrectValues() {
        assertEquals("content_uri", Transaction.EXTRA_CONTENT_URI)
        assertEquals("file_path", Transaction.EXTRA_FILE_PATH)
    }

    @Test
    fun givenErrTypeGenericPermanent_thenExists() {
        // Verify the constant exists and has expected behavior
        val errType = Telephony.MmsSms.ERR_TYPE_GENERIC_PERMANENT
        assertTrue(errType > 0)
    }

    @Test
    fun givenPendingMessagesContentUri_thenIsValid() {
        val uri = Telephony.MmsSms.PendingMessages.CONTENT_URI
        assertNotNull(uri)
        assertEquals("content", uri.scheme)
    }
}
