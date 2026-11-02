/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.klinker.android.send_message

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MmsHandoffTest {

    @Test
    fun resultIntentSurvivesBeingStoredForTheTimeout() {
        val intent =
            Intent(MmsReceivedReceiver.MMS_RECEIVED)
                .setComponent(ComponentName("org.prauga.messages", "org.prauga.messages.Receiver"))
                .putExtra(MmsReceivedReceiver.EXTRA_LOCATION_URL, "http://mmsc/x?id=1&a=b")
                .putExtra(MmsReceivedReceiver.EXTRA_URI, Uri.parse("content://mms/inbox/7"))
                .putExtra(MmsReceivedReceiver.EXTRA_TRIGGER_PUSH, true)
                .putExtra(MmsReceivedReceiver.EXTRA_SUBSCRIPTION_ID, 2)
                .putExtra(MmsHandoff.EXTRA_ATTEMPT, 42L)

        val decoded = MmsHandoff.decode(MmsHandoff.encode(intent))

        assertEquals(intent.action, decoded.action)
        assertEquals(intent.component, decoded.component)
        assertEquals(
            "http://mmsc/x?id=1&a=b",
            decoded.getStringExtra(MmsReceivedReceiver.EXTRA_LOCATION_URL),
        )
        assertEquals(
            Uri.parse("content://mms/inbox/7"),
            decoded.getParcelableExtra<Uri>(MmsReceivedReceiver.EXTRA_URI),
        )
        assertEquals(true, decoded.getBooleanExtra(MmsReceivedReceiver.EXTRA_TRIGGER_PUSH, false))
        assertEquals(2, decoded.getIntExtra(MmsReceivedReceiver.EXTRA_SUBSCRIPTION_ID, -1))
        assertEquals(42L, decoded.getLongExtra(MmsHandoff.EXTRA_ATTEMPT, -1))
        assertFalse(decoded.hasExtra("mms_handoff_uri_extras"))
    }
}
