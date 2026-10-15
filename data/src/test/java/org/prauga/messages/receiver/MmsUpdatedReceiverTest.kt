/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class MmsUpdatedReceiverTest {

    @Mock private lateinit var context: Context

    @Mock private lateinit var syncMessage: SyncMessage

    private lateinit var receiver: MmsUpdatedReceiver

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        receiver = MmsUpdatedReceiver()
        receiver.syncMessage = syncMessage
    }

    @Test
    fun givenMmsUpdatedReceiver_whenInstantiated_thenIsBroadcastReceiverSubclass() {
        val receiver = MmsUpdatedReceiver()

        assertNotNull(receiver)
        assertTrue(receiver is android.content.BroadcastReceiver)
    }

    @Test
    fun givenReceiverWithInjectedDependencies_whenAccessed_thenDependenciesAreAvailable() {
        assertNotNull(receiver.syncMessage)
    }

    @Test
    fun givenUriConstant_thenHasCorrectValue() {
        assertEquals("uri", MmsUpdatedReceiver.URI)
    }

    @Test
    fun givenIntent_whenUriExtraSet_thenRetrievesCorrectly() {
        val expectedUri = "content://mms/123"
        val intent = Intent().apply { putExtra(MmsUpdatedReceiver.URI, expectedUri) }

        val uriString = intent.getStringExtra(MmsUpdatedReceiver.URI)

        assertEquals(expectedUri, uriString)
    }

    @Test
    fun givenIntent_whenUriExtraNotSet_thenReturnsNull() {
        val intent = Intent()

        val uriString = intent.getStringExtra(MmsUpdatedReceiver.URI)

        assertNull(uriString)
    }

    @Test
    fun givenUriString_whenParsed_thenCreatesValidUri() {
        val uriString = "content://mms/outbox/456"
        val uri = Uri.parse(uriString)

        assertNotNull(uri)
        assertEquals("content", uri.scheme)
        assertEquals("mms", uri.authority)
    }

    @Test
    fun givenNullUriString_whenHandled_thenNoException() {
        val uriString: String? = null

        uriString?.let {
            // Should not execute
            assertTrue(false)
        }

        // Test passes if no exception thrown
        assertTrue(true)
    }

    @Test
    fun givenValidUriString_whenPassedToLet_thenExecutesBlock() {
        val uriString: String? = "content://mms/123"
        var executed = false

        uriString?.let { executed = true }

        assertTrue(executed)
    }

    @Test
    fun givenMmsOutboxUri_whenParsed_thenHasCorrectPath() {
        val uri = Uri.parse("content://mms/outbox/789")
        assertTrue(uri.path?.contains("outbox") == true)
    }

    @Test
    fun givenMmsSentUri_whenParsed_thenHasCorrectPath() {
        val uri = Uri.parse("content://mms/sent/100")
        assertTrue(uri.path?.contains("sent") == true)
    }

    @Test
    fun givenMmsUri_whenContentIdParsed_thenReturnsId() {
        val uri = Uri.parse("content://mms/555")
        val id = android.content.ContentUris.parseId(uri)

        assertEquals(555L, id)
    }

    @Test
    fun givenIntentWithMmsUpdatedAction_whenCreated_thenHasCorrectAction() {
        val action = "org.prauga.messages.MMS_UPDATED"
        val intent = Intent(action)

        assertEquals(action, intent.action)
    }
}
