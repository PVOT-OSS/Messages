/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import io.realm.Realm
import io.realm.RealmList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.prauga.messages.manager.KeyManager
import org.prauga.messages.mapper.CursorToContact
import org.prauga.messages.mapper.CursorToContactGroup
import org.prauga.messages.mapper.CursorToContactGroupMember
import org.prauga.messages.mapper.CursorToConversation
import org.prauga.messages.mapper.CursorToMessage
import org.prauga.messages.mapper.CursorToPart
import org.prauga.messages.mapper.CursorToRecipient
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import org.prauga.messages.model.Recipient
import org.prauga.messages.model.SyncLog
import org.prauga.messages.testutil.RealmTestHelper
import org.prauga.messages.util.PhoneNumberUtils

@RunWith(AndroidJUnit4::class)
class SyncMessagesInstrumentedTest {

    private lateinit var context: Context
    private lateinit var realm: Realm

    private val keys =
        object : KeyManager {
            private val ids = AtomicLong(1)

            override fun reset() = Unit

            override fun newId() = ids.getAndIncrement()
        }

    private class FailingCursor(columns: Array<String>, private val failAt: Int) :
        MatrixCursor(columns) {
        override fun onMove(oldPosition: Int, newPosition: Int): Boolean {
            check(newPosition < failAt) { "Provider went away" }
            return super.onMove(oldPosition, newPosition)
        }
    }

    private class FakeMessages(
        private val cursor: () -> Cursor,
        private val onRow: (Long) -> Unit,
    ) : CursorToMessage {
        override fun getMessagesCursor() = cursor()

        override fun getMessageCursor(id: Long): Cursor? = null

        override fun map(from: Pair<Cursor, CursorToMessage.MessageColumns>): Message {
            val id = from.first.getLong(0)
            onRow(id)
            return Message().apply {
                this.id = id
                threadId = 1 + id % 3
                type = "sms"
                body = "message $id"
                date = id
            }
        }
    }

    private val conversations =
        object : CursorToConversation {
            override fun getConversationsCursor() =
                MatrixCursor(arrayOf("_id")).apply { (1L..3L).forEach { addRow(arrayOf(it)) } }

            override fun map(from: Cursor) =
                from.getLong(0).let { id ->
                    Conversation(id = id, recipients = RealmList(Recipient(id, "555000$id")))
                }
        }

    private val noParts =
        object : CursorToPart {
            override fun getPartsCursor(messageId: Long?): Cursor? = null

            override fun map(from: Cursor) = MmsPart()
        }

    private val noRecipients =
        object : CursorToRecipient {
            override fun getRecipientCursor(): Cursor? = null

            override fun getRecipientCursor(id: Long): Cursor? = null

            override fun map(from: Cursor) = Recipient()
        }

    private val noContacts =
        object : CursorToContact {
            override fun getContactsCursor(): Cursor? = null

            override fun map(from: Cursor) = throw UnsupportedOperationException()
        }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        RealmTestHelper.init(context)
        realm = RealmTestHelper.getRealm()
    }

    @After
    fun tearDown() {
        realm.close()
        RealmTestHelper.tearDown()
    }

    private fun syncRepository(messages: CursorToMessage) =
        SyncRepositoryImpl(
            context.contentResolver,
            mock(ConversationRepository::class.java),
            conversations,
            messages,
            noParts,
            noRecipients,
            noContacts,
            mock(CursorToContactGroup::class.java),
            mock(CursorToContactGroupMember::class.java),
            keys,
            PhoneNumberUtils(context),
            RxSharedPreferences.create(context.getSharedPreferences("sync-test", 0)),
            EmojiReactionRepositoryImpl(context, keys, Moshi.Builder().build()),
        )

    private fun messagesCursor(count: Int, failAt: Int = Int.MAX_VALUE) =
        FailingCursor(arrayOf("_id"), failAt).apply {
            (1..count).forEach { addRow(arrayOf(it.toLong())) }
        }

    private fun SyncRepositoryImpl.syncAndWait() {
        syncMessages()
        syncProgress
            .filter { it is SyncRepository.SyncProgress.Idle }
            .timeout(2, TimeUnit.MINUTES)
            .blockingFirst()
    }

    private fun committedMessages(): Long {
        var count = 0L
        Thread { Realm.getDefaultInstance().use { count = it.where(Message::class.java).count() } }
            .apply {
                start()
                join()
            }
        return count
    }

    @Test
    fun givenHugeInbox_whenSyncing_thenMessagesAreCommittedInBatches() {
        var committedMidway = -1L
        val repository =
            syncRepository(
                FakeMessages({ messagesCursor(10_000) }) { id ->
                    if (id == 7_000L) committedMidway = committedMessages()
                }
            )

        repository.syncAndWait()

        assertThat(committedMidway).isAtLeast(6_000L)
        realm.refresh()
        assertThat(realm.where(Message::class.java).count()).isEqualTo(10_000L)
        assertThat(realm.where(Conversation::class.java).isNotNull("lastMessage").count())
            .isEqualTo(3L)
        assertThat(realm.where(SyncLog::class.java).count()).isEqualTo(1L)
    }

    @Test
    fun givenUserSettingsAndSyncFailsMidway_whenSyncing_thenSettingsSurviveAndSyncReruns() {
        realm.executeTransaction { r ->
            r.insert(Conversation(id = 2, pinned = true, archived = true, name = "Family"))
            r.insert(SyncLog())
        }
        val repository = syncRepository(FakeMessages({ messagesCursor(10_000, failAt = 5_000) }) {})

        repository.syncAndWait()

        realm.refresh()
        val family = realm.where(Conversation::class.java).equalTo("id", 2L).findFirst()!!
        assertThat(family.pinned).isTrue()
        assertThat(family.archived).isTrue()
        assertThat(family.name).isEqualTo("Family")
        assertThat(realm.where(SyncLog::class.java).count()).isEqualTo(0L)
    }

    @Test
    fun givenUserSettings_whenSyncSucceeds_thenSettingsAreKept() {
        realm.executeTransaction { r ->
            r.insert(Conversation(id = 2, pinned = true, name = "Family"))
        }
        val repository = syncRepository(FakeMessages({ messagesCursor(300) }) {})

        repository.syncAndWait()

        realm.refresh()
        val family = realm.where(Conversation::class.java).equalTo("id", 2L).findFirst()!!
        assertThat(family.pinned).isTrue()
        assertThat(family.name).isEqualTo("Family")
        assertThat(family.lastMessage).isNotNull()
    }
}
