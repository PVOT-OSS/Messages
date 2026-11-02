/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.interactor.DeleteMessages
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DeleteMessagesReceiverTest {

    @Test
    fun givenIdsFromAnOlderVersion_whenReceived_thenThoseMessagesAreDeleted() {
        val intent =
            Intent().putExtra("threadId", 100L).putExtra("messageIds", longArrayOf(1L, 2L, 3L))

        assertEquals(
            DeleteMessages.Params(listOf(1L, 2L, 3L), 100L),
            DeleteMessagesReceiver.params(intent),
        )
    }

    @Test
    fun givenADate_whenReceived_thenUnseenMessagesUpToItAreDeleted() {
        val intent =
            Intent()
                .putExtra("threadId", 100L)
                .putExtra(DeleteMessagesReceiver.EXTRA_UNSEEN_UNTIL, 1_000L)

        assertEquals(
            DeleteMessages.Params(emptyList(), 100L, unseenUntil = 1_000L),
            DeleteMessagesReceiver.params(intent),
        )
    }

    @Test
    fun givenNoExtras_whenReceived_thenNothingIsDeleted() {
        assertEquals(
            DeleteMessages.Params(emptyList(), 0L),
            DeleteMessagesReceiver.params(Intent()),
        )
    }
}
