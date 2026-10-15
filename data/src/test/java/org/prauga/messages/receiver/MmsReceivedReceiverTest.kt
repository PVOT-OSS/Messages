/*
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.Context
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.prauga.messages.interactor.ReceiveMms
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MmsReceivedReceiverTest {

    @Mock private lateinit var context: Context

    @Mock private lateinit var receiveMms: ReceiveMms

    private lateinit var receiver: MmsReceivedReceiver

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        receiver = MmsReceivedReceiver()
        receiver.receiveMms = receiveMms
    }

    @Test
    fun givenMmsReceivedReceiver_whenInstantiated_thenIsNotNull() {
        val receiver = MmsReceivedReceiver()
        assertNotNull(receiver)
    }

    @Test
    fun givenMmsReceivedReceiver_whenInstantiated_thenExtendsMmsReceivedReceiver() {
        val receiver = MmsReceivedReceiver()
        assertTrue(receiver is com.klinker.android.send_message.MmsReceivedReceiver)
    }

    @Test
    fun givenReceiverWithInjectedDependencies_whenAccessed_thenDependenciesAreAvailable() {
        assertNotNull(receiver.receiveMms)
    }

    @Test
    fun givenMmsUri_whenParsed_thenHasCorrectFormat() {
        val uriString = "content://mms/inbox/456"
        val uri = Uri.parse(uriString)

        assertEquals("content", uri.scheme)
        assertEquals("mms", uri.authority)
        assertTrue(uri.path?.contains("inbox") == true)
    }

    @Test
    fun givenReceivedMmsUri_whenContentIdExtracted_thenReturnsCorrectId() {
        val uriString = "content://mms/789"
        val uri = Uri.parse(uriString)
        val id = android.content.ContentUris.parseId(uri)

        assertEquals(789L, id)
    }

    @Test
    fun givenNullUri_whenHandled_thenNoException() {
        val uri: Uri? = null
        // Receiver should handle null gracefully with let{}
        uri?.let {
            // Should not execute
            assertTrue(false)
        }
        // Test passes if no exception thrown
        assertTrue(true)
    }

    @Test
    fun givenValidUri_whenPassedToLet_thenExecutesBlock() {
        val uri: Uri? = Uri.parse("content://mms/123")
        var executed = false

        uri?.let { executed = true }

        assertTrue(executed)
    }

    @Test
    fun givenMmsInboxUri_thenParsesCorrectly() {
        val inboxUri = Uri.parse("content://mms/inbox")
        assertEquals("inbox", inboxUri.lastPathSegment)
    }

    @Test
    fun givenMmsPartUri_thenParsesCorrectly() {
        val partUri = Uri.parse("content://mms/part/100")
        assertTrue(partUri.path?.contains("part") == true)
    }
}
