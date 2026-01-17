/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 *
 * Instrumented tests for Group MMS functionality.
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
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Message
import org.prauga.messages.testutil.InstrumentedMmsTestDataFactory
import org.prauga.messages.testutil.RealmTestHelper

/**
 * Instrumented tests for Group MMS functionality.
 * Tests group message creation, persistence, and retrieval.
 */
@RunWith(AndroidJUnit4::class)
class GroupMmsInstrumentedTest {

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

    // ==================== Group Message Creation Tests ====================

    @Test
    fun givenGroupMms_whenCreate_thenStoredInRealm() {
        val message = InstrumentedMmsTestDataFactory.createGroupMms(realm)

        assertThat(message.id).isGreaterThan(0L)
        assertThat(message.type).isEqualTo("mms")
    }

    @Test
    fun givenGroupMms_whenCreate_thenHasMultipleRecipients() {
        val recipients = listOf("+15551111111", "+15552222222", "+15553333333")
        val message = InstrumentedMmsTestDataFactory.createGroupMms(
            realm,
            recipients = recipients
        )

        assertThat(message.address).contains("+15551111111")
        assertThat(message.address).contains("+15552222222")
        assertThat(message.address).contains("+15553333333")
    }

    @Test
    fun givenGroupMms_whenCreate_thenBodyPreserved() {
        val message = InstrumentedMmsTestDataFactory.createGroupMms(
            realm,
            text = "Hello group!"
        )

        assertThat(message.body).isEqualTo("Hello group!")
    }

    @Test
    fun givenGroupMms_whenCreate_thenHasTextPart() {
        val message = InstrumentedMmsTestDataFactory.createGroupMms(realm)

        assertThat(message.parts).isNotEmpty()
        assertThat(message.parts[0]!!.type).isEqualTo("text/plain")
    }

    // ==================== Group Conversation Tests ====================

    @Test
    fun givenGroupConversation_whenCreate_thenStoredInRealm() {
        val conversation = InstrumentedMmsTestDataFactory.createGroupConversation(realm)

        assertThat(conversation.id).isGreaterThan(0L)
    }

    @Test
    fun givenGroupConversation_whenCreate_thenHasMultipleRecipients() {
        val addresses = listOf("+15551111111", "+15552222222", "+15553333333")
        val conversation = InstrumentedMmsTestDataFactory.createGroupConversation(
            realm,
            addresses = addresses
        )

        assertThat(conversation.recipients.size).isEqualTo(3)
    }

    @Test
    fun givenGroupConversation_whenRetrieve_thenRecipientsAccessible() {
        val addresses = listOf("+15551111111", "+15552222222")
        val conversation = InstrumentedMmsTestDataFactory.createGroupConversation(
            realm,
            addresses = addresses
        )
        val conversationId = conversation.id

        val retrieved = realm.where(Conversation::class.java)
            .equalTo("id", conversationId)
            .findFirst()

        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.recipients.size).isEqualTo(2)

        val recipientAddresses = retrieved.recipients.map { it.address }
        assertThat(recipientAddresses).contains("+15551111111")
        assertThat(recipientAddresses).contains("+15552222222")
    }

    // ==================== Thread Tests ====================

    @Test
    fun givenGroupMms_whenCreateWithThreadId_thenThreadIdSet() {
        val message = InstrumentedMmsTestDataFactory.createGroupMms(
            realm,
            threadId = 12345L
        )

        assertThat(message.threadId).isEqualTo(12345L)
    }

    @Test
    fun givenMultipleGroupMms_whenSameThread_thenGroupedTogether() {
        val threadId = 9999L

        InstrumentedMmsTestDataFactory.createGroupMms(
            realm,
            text = "Message 1",
            threadId = threadId
        )
        InstrumentedMmsTestDataFactory.createGroupMms(
            realm,
            text = "Message 2",
            threadId = threadId
        )
        InstrumentedMmsTestDataFactory.createGroupMms(
            realm,
            text = "Message 3",
            threadId = threadId
        )

        val messagesInThread = realm.where(Message::class.java)
            .equalTo("threadId", threadId)
            .findAll()

        assertThat(messagesInThread.size).isEqualTo(3)
    }

    // ==================== Recipient Count Tests ====================

    @Test
    fun givenTwoRecipients_whenCreateGroupMms_thenAddressContainsBoth() {
        val message = InstrumentedMmsTestDataFactory.createGroupMms(
            realm,
            recipients = listOf("+15551111111", "+15552222222")
        )

        assertThat(message.address).contains("+15551111111")
        assertThat(message.address).contains("+15552222222")
    }

    @Test
    fun givenFiveRecipients_whenCreateGroupMms_thenAddressContainsAll() {
        val recipients = listOf(
            "+15551111111",
            "+15552222222",
            "+15553333333",
            "+15554444444",
            "+15555555555"
        )
        val message = InstrumentedMmsTestDataFactory.createGroupMms(
            realm,
            recipients = recipients
        )

        recipients.forEach { recipient ->
            assertThat(message.address).contains(recipient)
        }
    }

    @Test
    fun givenTenRecipients_whenCreateGroupMms_thenAddressContainsAll() {
        val recipients = (1..10).map { "+1555000000$it" }
        val message = InstrumentedMmsTestDataFactory.createGroupMms(
            realm,
            recipients = recipients
        )

        recipients.forEach { recipient ->
            assertThat(message.address).contains(recipient)
        }
    }

    // ==================== Query Tests ====================

    @Test
    fun givenGroupMms_whenQueryByThread_thenFound() {
        val threadId = 7777L
        InstrumentedMmsTestDataFactory.createGroupMms(
            realm,
            text = "Group message",
            threadId = threadId
        )

        val found = realm.where(Message::class.java)
            .equalTo("threadId", threadId)
            .findFirst()

        assertThat(found).isNotNull()
        assertThat(found!!.body).isEqualTo("Group message")
    }

    @Test
    fun givenMultipleGroupConversations_whenQueryAll_thenAllFound() {
        InstrumentedMmsTestDataFactory.createGroupConversation(
            realm,
            addresses = listOf("+15551111111", "+15552222222")
        )
        InstrumentedMmsTestDataFactory.createGroupConversation(
            realm,
            addresses = listOf("+15553333333", "+15554444444")
        )

        val allConversations = realm.where(Conversation::class.java).findAll()

        assertThat(allConversations.size).isEqualTo(2)
    }

    // ==================== Incoming Group MMS Tests ====================

    @Test
    fun givenIncomingGroupMms_whenReceived_thenInInbox() {
        val message = InstrumentedMmsTestDataFactory.createGroupMms(realm)

        assertThat(message.boxId).isEqualTo(Mms.MESSAGE_BOX_INBOX)
    }

    @Test
    fun givenIncomingGroupMms_whenReceived_thenMarkedAsUnread() {
        val message = InstrumentedMmsTestDataFactory.createGroupMms(realm)

        assertThat(message.read).isFalse()
        assertThat(message.seen).isFalse()
    }

    // ==================== Outgoing Group MMS Tests ====================

    @Test
    fun givenOutgoingGroupMms_whenSent_thenInSentBox() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            address = "+15551111111, +15552222222"
        )

        assertThat(message.boxId).isEqualTo(Mms.MESSAGE_BOX_SENT)
    }

    @Test
    fun givenOutgoingGroupMms_whenSent_thenMarkedAsRead() {
        val message = InstrumentedMmsTestDataFactory.createOutgoingMms(
            realm,
            address = "+15551111111, +15552222222"
        )

        assertThat(message.read).isTrue()
        assertThat(message.seen).isTrue()
    }

    // ==================== Delete Tests ====================

    @Test
    fun givenGroupMms_whenDelete_thenRemoved() {
        val message = InstrumentedMmsTestDataFactory.createGroupMms(realm)
        val messageId = message.id

        realm.executeTransaction { r ->
            val toDelete = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            toDelete?.deleteFromRealm()
        }

        val found = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(found).isNull()
    }

    @Test
    fun givenGroupConversation_whenDelete_thenRemoved() {
        val conversation = InstrumentedMmsTestDataFactory.createGroupConversation(realm)
        val conversationId = conversation.id

        realm.executeTransaction { r ->
            val toDelete = r.where(Conversation::class.java)
                .equalTo("id", conversationId)
                .findFirst()
            toDelete?.deleteFromRealm()
        }

        val found = realm.where(Conversation::class.java)
            .equalTo("id", conversationId)
            .findFirst()

        assertThat(found).isNull()
    }

    // ==================== Update Tests ====================

    @Test
    fun givenGroupMms_whenMarkAsRead_thenUpdated() {
        val message = InstrumentedMmsTestDataFactory.createGroupMms(realm)
        val messageId = message.id

        realm.executeTransaction { r ->
            val toUpdate = r.where(Message::class.java)
                .equalTo("id", messageId)
                .findFirst()
            toUpdate?.read = true
            toUpdate?.seen = true
        }

        val updated = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(updated!!.read).isTrue()
        assertThat(updated.seen).isTrue()
    }

    // ==================== Retrieval Tests ====================

    @Test
    fun givenGroupMms_whenRetrieve_thenDataComplete() {
        val recipients = listOf("+15551111111", "+15552222222", "+15553333333")
        val original = InstrumentedMmsTestDataFactory.createGroupMms(
            realm,
            recipients = recipients,
            text = "Group chat message",
            threadId = 5555L
        )
        val messageId = original.id

        val retrieved = realm.where(Message::class.java)
            .equalTo("id", messageId)
            .findFirst()

        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.body).isEqualTo("Group chat message")
        assertThat(retrieved.threadId).isEqualTo(5555L)
        assertThat(retrieved.type).isEqualTo("mms")

        recipients.forEach { recipient ->
            assertThat(retrieved.address).contains(recipient)
        }
    }

    // ==================== Mixed Content Tests ====================

    @Test
    fun givenGroupMmsWithImage_whenCreate_thenHasBothParts() {
        val message = InstrumentedMmsTestDataFactory.createImageMms(
            realm,
            text = "Group photo!",
            address = "+15551111111, +15552222222"
        )

        assertThat(message.parts.size).isEqualTo(2)

        val hasTextPart = message.parts.any { it.type == "text/plain" }
        val hasImagePart = message.parts.any { it.type?.startsWith("image/") == true }

        assertThat(hasTextPart).isTrue()
        assertThat(hasImagePart).isTrue()
    }

    // ==================== Conversation Count Tests ====================

    @Test
    fun givenConversation_whenCountRecipients_thenCorrect() {
        val addresses = listOf("+15551111111", "+15552222222", "+15553333333")
        val conversation = InstrumentedMmsTestDataFactory.createGroupConversation(
            realm,
            addresses = addresses
        )

        assertThat(conversation.recipients.size).isEqualTo(3)
    }

    @Test
    fun givenSingleRecipientConversation_whenCheck_thenNotGroup() {
        val conversation = InstrumentedMmsTestDataFactory.createConversation(
            realm,
            address = "+15551111111"
        )

        assertThat(conversation.recipients.size).isEqualTo(1)
    }
}
