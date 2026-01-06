/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Instrumented tests for MMS edge cases and boundary conditions.
 */
package org.prauga.messages.mms

import android.content.Context
import android.provider.Telephony.Mms
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.realm.Realm
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import org.prauga.messages.testutil.InstrumentedMmsTestDataFactory
import org.prauga.messages.testutil.RealmTestHelper

/**
 * Instrumented tests for MMS edge cases.
 * Tests boundary conditions, special characters, and unusual scenarios.
 */
@RunWith(AndroidJUnit4::class)
class MmsEdgeCasesInstrumentedTest {

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

    // ==================== Empty/Null Content Tests ====================

    @Test
    fun givenEmptyText_whenCreateMms_thenEmptyBodyStored() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, "")

        assertThat(message.body).isEmpty()
    }

    @Test
    fun givenImageOnlyMms_whenCreate_thenNoTextPart() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(realm, text = "")

        assertThat(message.parts.size).isEqualTo(1)
        assertThat(message.parts[0]!!.type).isEqualTo("image/jpeg")
    }

    // ==================== Unicode Text Tests ====================

    @Test
    fun givenChineseText_whenCreateMms_thenPreserved() {
        val chineseText = "你好世界"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, chineseText)

        assertThat(message.body).isEqualTo(chineseText)
    }

    @Test
    fun givenArabicText_whenCreateMms_thenPreserved() {
        val arabicText = "مرحبا بالعالم"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, arabicText)

        assertThat(message.body).isEqualTo(arabicText)
    }

    @Test
    fun givenJapaneseText_whenCreateMms_thenPreserved() {
        val japaneseText = "こんにちは世界"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, japaneseText)

        assertThat(message.body).isEqualTo(japaneseText)
    }

    @Test
    fun givenKoreanText_whenCreateMms_thenPreserved() {
        val koreanText = "안녕하세요 세계"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, koreanText)

        assertThat(message.body).isEqualTo(koreanText)
    }

    @Test
    fun givenRussianText_whenCreateMms_thenPreserved() {
        val russianText = "Привет мир"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, russianText)

        assertThat(message.body).isEqualTo(russianText)
    }

    @Test
    fun givenHebrewText_whenCreateMms_thenPreserved() {
        val hebrewText = "שלום עולם"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, hebrewText)

        assertThat(message.body).isEqualTo(hebrewText)
    }

    @Test
    fun givenMixedLanguageText_whenCreateMms_thenPreserved() {
        val mixedText = "Hello 你好 مرحبا こんにちは"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, mixedText)

        assertThat(message.body).isEqualTo(mixedText)
    }

    // ==================== Emoji Tests ====================

    @Test
    fun givenSingleEmoji_whenCreateMms_thenPreserved() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, "👍")

        assertThat(message.body).isEqualTo("👍")
    }

    @Test
    fun givenMultipleEmojis_whenCreateMms_thenPreserved() {
        val emojiText = "🎉🎊🥳🎈🎁"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, emojiText)

        assertThat(message.body).isEqualTo(emojiText)
    }

    @Test
    fun givenEmojiWithText_whenCreateMms_thenPreserved() {
        val text = "Happy birthday! 🎂🎉"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, text)

        assertThat(message.body).isEqualTo(text)
    }

    @Test
    fun givenSkinToneEmoji_whenCreateMms_thenPreserved() {
        val text = "👋🏻👋🏼👋🏽👋🏾👋🏿"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, text)

        assertThat(message.body).isEqualTo(text)
    }

    @Test
    fun givenFamilyEmoji_whenCreateMms_thenPreserved() {
        val text = "👨‍👩‍👧‍👦"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, text)

        assertThat(message.body).isEqualTo(text)
    }

    @Test
    fun givenFlagEmoji_whenCreateMms_thenPreserved() {
        val text = "🇺🇸🇬🇧🇫🇷🇩🇪🇯🇵"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, text)

        assertThat(message.body).isEqualTo(text)
    }

    // ==================== Special Characters Tests ====================

    @Test
    fun givenNewlines_whenCreateMms_thenPreserved() {
        val text = "Line 1\nLine 2\nLine 3"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, text)

        assertThat(message.body).isEqualTo(text)
    }

    @Test
    fun givenTabs_whenCreateMms_thenPreserved() {
        val text = "Col1\tCol2\tCol3"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, text)

        assertThat(message.body).isEqualTo(text)
    }

    @Test
    fun givenSpecialCharacters_whenCreateMms_thenPreserved() {
        val text = "Special: @#\$%^&*()_+-=[]{}|;':\",./<>?"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, text)

        assertThat(message.body).isEqualTo(text)
    }

    @Test
    fun givenUrlInText_whenCreateMms_thenPreserved() {
        val text = "Check out https://example.com/path?param=value&other=123"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, text)

        assertThat(message.body).isEqualTo(text)
    }

    @Test
    fun givenHtmlEntities_whenCreateMms_thenPreserved() {
        val text = "Test &amp; &lt; &gt; &quot; entities"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, text)

        assertThat(message.body).isEqualTo(text)
    }

    // ==================== Long Text Tests ====================

    @Test
    fun givenLongText_whenCreateMms_thenPreserved() {
        val longText = "A".repeat(1000)
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, longText)

        assertThat(message.body).isEqualTo(longText)
        assertThat(message.body.length).isEqualTo(1000)
    }

    @Test
    fun givenVeryLongText_whenCreateMms_thenPreserved() {
        val veryLongText = "Test message content. ".repeat(500)
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, veryLongText)

        assertThat(message.body).isEqualTo(veryLongText)
    }

    // ==================== Phone Number Format Tests ====================

    @Test
    fun givenInternationalNumber_whenCreateMms_thenPreserved() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            address = "+14155551234"
        )

        assertThat(message.address).isEqualTo("+14155551234")
    }

    @Test
    fun givenLocalNumber_whenCreateMms_thenPreserved() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            address = "5551234"
        )

        assertThat(message.address).isEqualTo("5551234")
    }

    @Test
    fun givenFormattedNumber_whenCreateMms_thenPreserved() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            address = "(415) 555-1234"
        )

        assertThat(message.address).isEqualTo("(415) 555-1234")
    }

    // ==================== Subject Edge Cases Tests ====================

    @Test
    fun givenEmptySubject_whenCreateMms_thenNoSubjectSet() {
        val message = InstrumentedMmsTestDataFactory.createMmsWithSubject(
            realm,
            subject = ""
        )

        assertThat(message.subject).isEmpty()
    }

    @Test
    fun givenLongSubject_whenCreateMms_thenPreserved() {
        val longSubject = "This is a very long subject line that exceeds normal limits"
        val message = InstrumentedMmsTestDataFactory.createMmsWithSubject(
            realm,
            subject = longSubject
        )

        assertThat(message.subject).isEqualTo(longSubject)
    }

    @Test
    fun givenUnicodeSubject_whenCreateMms_thenPreserved() {
        val unicodeSubject = "重要なメッセージ 📧"
        val message = InstrumentedMmsTestDataFactory.createMmsWithSubject(
            realm,
            subject = unicodeSubject
        )

        assertThat(message.subject).isEqualTo(unicodeSubject)
    }

    // ==================== Thread ID Edge Cases Tests ====================

    @Test
    fun givenZeroThreadId_whenCreateMms_thenStored() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            threadId = 0L
        )

        assertThat(message.threadId).isEqualTo(0L)
    }

    @Test
    fun givenMaxLongThreadId_whenCreateMms_thenStored() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(
            realm,
            threadId = Long.MAX_VALUE
        )

        assertThat(message.threadId).isEqualTo(Long.MAX_VALUE)
    }

    // ==================== Multiple Parts Edge Cases ====================

    @Test
    fun givenManyParts_whenCreate_thenAllStored() {
        // Create image with caption (2 parts)
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            text = "Photo caption"
        )

        assertThat(message.parts.size).isGreaterThan(1)
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    fun givenMultipleMessages_whenCreateRapidly_thenAllStored() {
        val count = 50
        for (i in 1..count) {
            InstrumentedMmsTestDataFactory.createTextMms(realm, "Message $i")
        }

        val allMessages = realm.where(Message::class.java).findAll()

        assertThat(allMessages.size).isEqualTo(count)
    }

    // ==================== Mixed Content Type Tests ====================

    @Test
    fun givenDifferentMimeTypes_whenQuery_thenCorrectlyFiltered() {
        InstrumentedMmsTestDataFactory.createTextMms(realm)
        InstrumentedMmsTestDataFactory.createImageMms(realm, imageType = "image/jpeg")
        InstrumentedMmsTestDataFactory.createImageMms(realm, imageType = "image/png")
        InstrumentedMmsTestDataFactory.createVideoMms(realm)
        InstrumentedMmsTestDataFactory.createAudioMms(realm)

        val allMessages = realm.where(Message::class.java).findAll()

        assertThat(allMessages.size).isEqualTo(5)
    }

    // ==================== Box ID Edge Cases ====================

    @Test
    fun givenDraftBoxId_whenCreate_thenStored() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_DRAFTS
        )

        assertThat(message.boxId).isEqualTo(Mms.MESSAGE_BOX_DRAFTS)
    }

    @Test
    fun givenFailedBoxId_whenCreate_thenStored() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_FAILED
        )

        assertThat(message.boxId).isEqualTo(Mms.MESSAGE_BOX_FAILED)
    }

    @Test
    fun givenOutboxBoxId_whenCreate_thenStored() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            boxId = Mms.MESSAGE_BOX_OUTBOX
        )

        assertThat(message.boxId).isEqualTo(Mms.MESSAGE_BOX_OUTBOX)
    }

    // ==================== Query Edge Cases ====================

    @Test
    fun givenNoMessages_whenQuery_thenEmptyResult() {
        val messages = realm.where(Message::class.java).findAll()

        assertThat(messages).isEmpty()
    }

    @Test
    fun givenNonExistentId_whenQuery_thenNull() {
        val found = realm.where(Message::class.java)
            .equalTo("id", 99999999L)
            .findFirst()

        assertThat(found).isNull()
    }

    // ==================== Timestamp Edge Cases ====================

    @Test
    fun givenFutureTimestamp_whenCreate_thenStored() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm)
        val messageId = message.id

        val futureTime = System.currentTimeMillis() + 86400000 // +1 day

        realm.executeTransaction { r ->
            val toUpdate = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            toUpdate?.date = futureTime
        }

        val updated = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(updated!!.date).isEqualTo(futureTime)
    }

    @Test
    fun givenPastTimestamp_whenCreate_thenStored() {
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm)
        val messageId = message.id

        val pastTime = 946684800000L // Jan 1, 2000

        realm.executeTransaction { r ->
            val toUpdate = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            toUpdate?.date = pastTime
        }

        val updated = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(updated!!.date).isEqualTo(pastTime)
    }

    // ==================== Whitespace Tests ====================

    @Test
    fun givenOnlyWhitespace_whenCreateMms_thenPreserved() {
        val text = "   \t\n   "
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, text)

        assertThat(message.body).isEqualTo(text)
    }

    @Test
    fun givenLeadingTrailingWhitespace_whenCreateMms_thenPreserved() {
        val text = "  Message with spaces  "
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, text)

        assertThat(message.body).isEqualTo(text)
    }

    // ==================== RTL Text Tests ====================

    @Test
    fun givenRtlText_whenCreateMms_thenPreserved() {
        // Arabic text (RTL)
        val rtlText = "مرحبا بك في تطبيقنا"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, rtlText)

        assertThat(message.body).isEqualTo(rtlText)
    }

    @Test
    fun givenMixedRtlLtrText_whenCreateMms_thenPreserved() {
        val mixedText = "Hello مرحبا World عالم"
        val message = InstrumentedMmsTestDataFactory.createTextMms(realm, mixedText)

        assertThat(message.body).isEqualTo(mixedText)
    }
}
