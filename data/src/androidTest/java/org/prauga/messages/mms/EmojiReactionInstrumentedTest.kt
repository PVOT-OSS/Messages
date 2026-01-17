/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Instrumented tests for Emoji reactions on MMS messages.
 */
package org.prauga.messages.mms

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.realm.Realm
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.model.EmojiReaction
import org.prauga.messages.model.Message
import org.prauga.messages.testutil.InstrumentedMmsTestDataFactory
import org.prauga.messages.testutil.RealmTestHelper

/**
 * Instrumented tests for Emoji reactions.
 * Tests reaction creation, persistence, and retrieval.
 */
@RunWith(AndroidJUnit4::class)
class EmojiReactionInstrumentedTest {

    private lateinit var context: Context
    private lateinit var realm: Realm

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        RealmTestHelper.init(context)
        realm = RealmTestHelper.getRealm()
        InstrumentedMmsTestDataFactory.resetIdGenerators()
    }

    @After
    fun tearDown() {
        realm.close()
        RealmTestHelper.clearAll()
        RealmTestHelper.tearDown()
    }

    // ==================== Basic Reaction Tests ====================

    @Test
    fun givenEmojiReaction_whenCreate_thenStoredInRealm() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(realm)

        assertThat(reaction.id).isGreaterThan(0L)
    }

    @Test
    fun givenEmojiReaction_whenCreate_thenEmojiPreserved() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "👍"
        )

        assertThat(reaction.emoji).isEqualTo("👍")
    }

    @Test
    fun givenEmojiReaction_whenCreate_thenSenderAddressSet() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            senderAddress = "+15559876543"
        )

        assertThat(reaction.senderAddress).isEqualTo("+15559876543")
    }

    @Test
    fun givenEmojiReaction_whenCreate_thenThreadIdSet() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            threadId = 1234L
        )

        assertThat(reaction.threadId).isEqualTo(1234L)
    }

    // ==================== Common Emoji Tests ====================

    @Test
    fun givenThumbsUpReaction_whenCreate_thenPreserved() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "👍"
        )

        assertThat(reaction.emoji).isEqualTo("👍")
    }

    @Test
    fun givenThumbsDownReaction_whenCreate_thenPreserved() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "👎"
        )

        assertThat(reaction.emoji).isEqualTo("👎")
    }

    @Test
    fun givenHeartReaction_whenCreate_thenPreserved() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "❤️"
        )

        assertThat(reaction.emoji).isEqualTo("❤️")
    }

    @Test
    fun givenLaughingReaction_whenCreate_thenPreserved() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "😂"
        )

        assertThat(reaction.emoji).isEqualTo("😂")
    }

    @Test
    fun givenSurprisedReaction_whenCreate_thenPreserved() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "😮"
        )

        assertThat(reaction.emoji).isEqualTo("😮")
    }

    @Test
    fun givenSadReaction_whenCreate_thenPreserved() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "😢"
        )

        assertThat(reaction.emoji).isEqualTo("😢")
    }

    @Test
    fun givenAngryReaction_whenCreate_thenPreserved() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "😠"
        )

        assertThat(reaction.emoji).isEqualTo("😠")
    }

    // ==================== Query Tests ====================

    @Test
    fun givenEmojiReaction_whenRetrieve_thenDataComplete() {
        val original = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "🎉",
            senderAddress = "+15557654321",
            threadId = 9999L
        )
        val reactionId = original.id

        val retrieved = realm.where(EmojiReaction::class.java)
            .equalTo("id", reactionId)
            .findFirst()

        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.emoji).isEqualTo("🎉")
        assertThat(retrieved.senderAddress).isEqualTo("+15557654321")
        assertThat(retrieved.threadId).isEqualTo(9999L)
    }

    @Test
    fun givenMultipleReactions_whenQueryByThread_thenAllFound() {
        val threadId = 5555L
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm, emoji = "👍", threadId = threadId)
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm, emoji = "❤️", threadId = threadId)
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm, emoji = "😂", threadId = threadId)

        val reactions = realm.where(EmojiReaction::class.java)
            .equalTo("threadId", threadId)
            .findAll()

        assertThat(reactions.size).isEqualTo(3)
    }

    @Test
    fun givenReactionsFromDifferentSenders_whenQueryBySender_thenFiltered() {
        InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            senderAddress = "+15551111111",
            threadId = 100L
        )
        InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            senderAddress = "+15552222222",
            threadId = 100L
        )
        InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            senderAddress = "+15551111111",
            threadId = 100L
        )

        val fromSender = realm.where(EmojiReaction::class.java)
            .equalTo("senderAddress", "+15551111111")
            .findAll()

        assertThat(fromSender.size).isEqualTo(2)
    }

    @Test
    fun givenReactionsOfSameType_whenQueryByEmoji_thenFiltered() {
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm, emoji = "👍")
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm, emoji = "👍")
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm, emoji = "❤️")

        val thumbsUp = realm.where(EmojiReaction::class.java)
            .equalTo("emoji", "👍")
            .findAll()

        assertThat(thumbsUp.size).isEqualTo(2)
    }

    // ==================== Delete Tests ====================

    @Test
    fun givenEmojiReaction_whenDelete_thenRemoved() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(realm)
        val reactionId = reaction.id

        realm.executeTransaction { r ->
            val toDelete = r.where(EmojiReaction::class.java)
                .equalTo("id", reactionId)
                .findFirst()
            toDelete?.deleteFromRealm()
        }

        val found = realm.where(EmojiReaction::class.java)
            .equalTo("id", reactionId)
            .findFirst()

        assertThat(found).isNull()
    }

    @Test
    fun givenMultipleReactions_whenDeleteOne_thenOthersRemain() {
        val reaction1 = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "👍",
            threadId = 100L
        )
        val reaction2 = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "❤️",
            threadId = 100L
        )
        val reaction2Id = reaction2.id

        realm.executeTransaction { r ->
            val toDelete = r.where(EmojiReaction::class.java)
                .equalTo("id", reaction2Id)
                .findFirst()
            toDelete?.deleteFromRealm()
        }

        val remaining = realm.where(EmojiReaction::class.java)
            .equalTo("threadId", 100L)
            .findAll()

        assertThat(remaining.size).isEqualTo(1)
        assertThat(remaining[0]!!.emoji).isEqualTo("👍")
    }

    @Test
    fun givenAllReactionsInThread_whenDeleteAll_thenNoneRemain() {
        val threadId = 777L
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm, threadId = threadId)
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm, threadId = threadId)
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm, threadId = threadId)

        realm.executeTransaction { r ->
            val toDelete = r.where(EmojiReaction::class.java)
                .equalTo("threadId", threadId)
                .findAll()
            toDelete.deleteAllFromRealm()
        }

        val remaining = realm.where(EmojiReaction::class.java)
            .equalTo("threadId", threadId)
            .findAll()

        assertThat(remaining.size).isEqualTo(0)
    }

    // ==================== Update Tests ====================

    @Test
    fun givenEmojiReaction_whenUpdateEmoji_thenChanged() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "👍"
        )
        val reactionId = reaction.id

        realm.executeTransaction { r ->
            val toUpdate = r.where(EmojiReaction::class.java)
                .equalTo("id", reactionId)
                .findFirst()
            toUpdate?.emoji = "❤️"
        }

        val updated = realm.where(EmojiReaction::class.java)
            .equalTo("id", reactionId)
            .findFirst()

        assertThat(updated!!.emoji).isEqualTo("❤️")
    }

    // ==================== Complex Emoji Tests ====================

    @Test
    fun givenComplexEmoji_whenCreate_thenPreserved() {
        // Emoji with skin tone modifier
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "👍🏽"
        )

        assertThat(reaction.emoji).isEqualTo("👍🏽")
    }

    @Test
    fun givenFlagEmoji_whenCreate_thenPreserved() {
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "🇺🇸"
        )

        assertThat(reaction.emoji).isEqualTo("🇺🇸")
    }

    @Test
    fun givenZwjEmoji_whenCreate_thenPreserved() {
        // Zero-width joiner emoji (family)
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "👨‍👩‍👧‍👦"
        )

        assertThat(reaction.emoji).isEqualTo("👨‍👩‍👧‍👦")
    }

    // ==================== Count Tests ====================

    @Test
    fun givenNoReactions_whenCount_thenZero() {
        val count = realm.where(EmojiReaction::class.java).count()

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun givenMultipleReactions_whenCount_thenCorrect() {
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm)
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm)
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm)
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm)
        InstrumentedMmsTestDataFactory.createEmojiReaction(realm)

        val count = realm.where(EmojiReaction::class.java).count()

        assertThat(count).isEqualTo(5)
    }

    // ==================== Message with Reaction Tests ====================

    @Test
    fun givenMessageAndReaction_whenCreated_thenBothPersisted() {
        val threadId = 3333L
        val message = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            text = "React to this!",
            threadId = threadId
        )
        val reaction = InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "👍",
            threadId = threadId
        )

        val foundMessage = realm.where(Message::class.java)
            .equalTo("threadId", threadId)
            .findFirst()
        val foundReaction = realm.where(EmojiReaction::class.java)
            .equalTo("threadId", threadId)
            .findFirst()

        assertThat(foundMessage).isNotNull()
        assertThat(foundReaction).isNotNull()
        assertThat(foundMessage!!.body).isEqualTo("React to this!")
        assertThat(foundReaction!!.emoji).isEqualTo("👍")
    }

    @Test
    fun givenMessageWithMultipleReactions_whenQuery_thenAllReactionsFound() {
        val threadId = 4444L
        InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            text = "Popular message",
            threadId = threadId
        )
        InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "👍",
            senderAddress = "+15551111111",
            threadId = threadId
        )
        InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "❤️",
            senderAddress = "+15552222222",
            threadId = threadId
        )
        InstrumentedMmsTestDataFactory.createEmojiReaction(
            realm,
            emoji = "😂",
            senderAddress = "+15553333333",
            threadId = threadId
        )

        val reactions = realm.where(EmojiReaction::class.java)
            .equalTo("threadId", threadId)
            .findAll()

        assertThat(reactions.size).isEqualTo(3)

        val emojis = reactions.map { it.emoji }
        assertThat(emojis).contains("👍")
        assertThat(emojis).contains("❤️")
        assertThat(emojis).contains("😂")
    }
}
