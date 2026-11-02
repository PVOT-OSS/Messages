/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import io.realm.Realm
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.prauga.messages.extensions.MESSAGE_PAGE_SIZE
import org.prauga.messages.manager.ActiveConversationManager
import org.prauga.messages.manager.KeyManager
import org.prauga.messages.mapper.CursorToPart
import org.prauga.messages.model.Message
import org.prauga.messages.testutil.RealmTestHelper
import org.prauga.messages.util.PhoneNumberUtils
import org.prauga.messages.util.Preferences
import org.prauga.messages.util.SmsSettings

@RunWith(AndroidJUnit4::class)
class MessageRepositoryScaleInstrumentedTest {

    private lateinit var context: Context
    private lateinit var realm: Realm
    private lateinit var repository: MessageRepositoryImpl

    private val keys =
        object : KeyManager {
            private val ids = AtomicLong(1)

            override fun reset() = Unit

            override fun newId() = ids.getAndIncrement()
        }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        RealmTestHelper.init(context)
        realm = RealmTestHelper.getRealm()
        val sharedPrefs = context.getSharedPreferences("message-repo-test", 0)
        val prefs = Preferences(context, RxSharedPreferences.create(sharedPrefs), sharedPrefs)
        repository =
            MessageRepositoryImpl(
                mock(ActiveConversationManager::class.java),
                context,
                mock(CursorToPart::class.java),
                keys,
                PhoneNumberUtils(context),
                prefs,
                mock(SyncRepository::class.java),
                EmojiReactionRepositoryImpl(context, keys, Moshi.Builder().build()),
                SmsSettings(context, prefs),
            )
    }

    @After
    fun tearDown() {
        realm.close()
        RealmTestHelper.tearDown()
    }

    private fun sms(id: Long, threadId: Long, date: Long, seen: Boolean = true) =
        Message().apply {
            this.id = id
            this.threadId = threadId
            type = "sms"
            body = "message $id"
            this.date = date
            this.seen = seen
            read = seen
        }

    private fun committedWhile(action: () -> Unit, count: (Realm) -> Long): Long {
        val done = AtomicBoolean(false)
        var seenBeforeDone = 0L
        val watcher = Thread {
            while (!done.get()) {
                Realm.getInstance(realm.configuration).use {
                    val seen = count(it)
                    if (!done.get()) seenBeforeDone = maxOf(seenBeforeDone, seen)
                }
            }
        }
        watcher.start()
        action()
        done.set(true)
        watcher.join()
        return seenBeforeDone
    }

    @Test
    fun givenHugeUnseenInbox_whenMarkingAllSeen_thenCommittedInBatchesUntilNoneLeft() {
        realm.executeTransaction { r ->
            r.insert((1L..10_000L).map { sms(it, it % 50, it, seen = false) })
        }

        val seenBeforeDone =
            committedWhile({ repository.markAllSeen() }) {
                it.where(Message::class.java).equalTo("seen", true).count()
            }

        assertThat(seenBeforeDone).isAtLeast(MESSAGE_PAGE_SIZE)
        realm.refresh()
        assertThat(realm.where(Message::class.java).equalTo("seen", false).count()).isEqualTo(0L)
    }

    @Test
    fun givenUnreadThreads_whenMarkingRead_thenOnlyThoseThreadsAreRead() {
        realm.executeTransaction { r ->
            r.insert((1L..6_000L).map { sms(it, it % 3, it, seen = false) })
        }

        repository.markRead(listOf(0L, 1L))

        realm.refresh()
        val unread = realm.where(Message::class.java).equalTo("read", false).findAll()
        assertThat(unread.map { it.threadId }.toSet()).containsExactly(2L)
        assertThat(unread).hasSize(2_000)
    }

    @Test
    fun givenManyOldMessagesSharingADate_whenCounting_thenEveryMessageIsCountedOnce() {
        val old = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        realm.executeTransaction { r ->
            r.insert((1L..5_000L).map { sms(it, 1 + it % 2, old) })
            r.insert((5_001L..5_100L).map { sms(it, 1, System.currentTimeMillis()) })
        }

        val counts = repository.getOldMessageCounts(7)

        assertThat(counts).containsExactly(1L, 2_500, 2L, 2_500)
    }

    @Test
    fun givenManyOldMessages_whenDeletingThem_thenOnlyOldOnesAreGone() {
        val old = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        realm.executeTransaction { r ->
            r.insert((1L..5_000L).map { sms(it, 1, old + it) })
            r.insert((5_001L..5_010L).map { sms(it, 1, System.currentTimeMillis()) })
        }

        repository.deleteOldMessages(7)

        realm.refresh()
        assertThat(realm.where(Message::class.java).count()).isEqualTo(10L)
    }

    @Test
    fun givenSelectionLargerThanAPage_whenDeleting_thenExactlyThoseAreGone() {
        realm.executeTransaction { r -> r.insert((1L..6_000L).map { sms(it, 1, it) }) }

        repository.deleteMessages((1L..5_000L).toList())

        realm.refresh()
        val left = realm.where(Message::class.java).findAll().map { it.id }
        assertThat(left).containsExactlyElementsIn(5_001L..6_000L)
    }
}
