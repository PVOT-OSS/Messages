/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.os.Debug
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import io.realm.Realm
import java.util.concurrent.atomic.AtomicLong
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.manager.KeyManager
import org.prauga.messages.model.EmojiReaction
import org.prauga.messages.model.Message
import org.prauga.messages.testutil.RealmTestHelper
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class EmojiReactionReparseInstrumentedTest {

    private lateinit var realm: Realm
    private lateinit var repository: EmojiReactionRepositoryImpl

    private val ids = AtomicLong(1)
    private val keys =
        object : KeyManager {
            override fun reset() = Unit

            override fun newId() = ids.getAndIncrement()
        }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        RealmTestHelper.init(context)
        realm = RealmTestHelper.getRealm()
        repository = EmojiReactionRepositoryImpl(context, keys, Moshi.Builder().build())
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
        realm.close()
        RealmTestHelper.tearDown()
    }

    private fun sms(id: Long, body: String, date: Long, reaction: Boolean = false) =
        Message().apply {
            this.id = id
            threadId = 1
            type = "sms"
            this.body = body
            this.date = date
            isEmojiReaction = reaction
        }

    private fun bytesAllocated() = Debug.getRuntimeStat("art.gc.bytes-allocated").toLong()

    @Test
    fun givenHugeInbox_whenClearingReactionFlags_thenMemoryUseDoesNotGrowWithTheInbox() {
        val count = 100_000
        realm.executeTransaction { r ->
            r.insert((1..count).map { sms(it.toLong(), "message $it", it.toLong(), it % 2000 == 0) })
        }

        realm.beginTransaction()
        val before = bytesAllocated()
        repository.clearEmojiReactionFlags(realm)
        val allocated = bytesAllocated() - before
        realm.commitTransaction()

        assertThat(realm.where(Message::class.java).equalTo("isEmojiReaction", true).count())
            .isEqualTo(0)
        assertThat(allocated).isLessThan(1_000_000L)
    }

    @Test
    fun givenReactionToAnEarlierMessage_whenReparsing_thenReactionIsLinkedAndFlagged() {
        val reaction = " Reacted ​👍​ to  hello there  "
        realm.executeTransaction { r ->
            r.insert(sms(1, "hello there", 1))
            r.insert(sms(2, reaction, 2))
            r.insert(sms(3, "unrelated", 3, reaction = true))
        }

        realm.executeTransaction { r -> repository.deleteAndReparseAllEmojiReactions(r) }

        val saved = realm.where(EmojiReaction::class.java).findAll()
        assertThat(saved).hasSize(1)
        assertThat(saved.first()!!.emoji).isEqualTo("👍")
        val flagged = realm.where(Message::class.java).equalTo("isEmojiReaction", true).findAll()
        assertThat(flagged.map { it.id }).containsExactly(2L)
    }

    @Test
    fun givenHugeInboxWithReleaseLogging_whenReparsing_thenEachMessageCostsLittleMemory() {
        Timber.plant(Timber.DebugTree())
        val count = 20_000
        realm.executeTransaction { r ->
            r.insert((1..count).map { sms(it.toLong(), "ordinary message $it", it.toLong()) })
        }

        realm.beginTransaction()
        val before = bytesAllocated()
        repository.deleteAndReparseAllEmojiReactions(realm)
        val perMessage = (bytesAllocated() - before) / count
        realm.commitTransaction()

        assertThat(perMessage).isLessThan(PER_MESSAGE_LIMIT)
    }

    private companion object {
        const val PER_MESSAGE_LIMIT = 4_000L
    }
}
