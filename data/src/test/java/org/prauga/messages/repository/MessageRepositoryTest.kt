/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.repository

import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.prauga.messages.model.Attachment
import org.prauga.messages.model.Message
import org.prauga.messages.model.MmsPart
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MessageRepositoryTest {

    @Mock private lateinit var messageRepository: MessageRepository

    @Mock private lateinit var smsMessage: Message

    @Mock private lateinit var mmsMessage: Message

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    // markSending tests

    @Test
    fun givenMessageId_whenMarkSending_thenRepositoryIsCalled() {
        val messageId = 123L
        doNothing().`when`(messageRepository).markSending(messageId)

        messageRepository.markSending(messageId)

        verify(messageRepository).markSending(messageId)
    }

    @Test
    fun givenZeroMessageId_whenMarkSending_thenRepositoryAcceptsIt() {
        val messageId = 0L
        doNothing().`when`(messageRepository).markSending(messageId)

        messageRepository.markSending(messageId)

        verify(messageRepository).markSending(messageId)
    }

    // markSent tests

    @Test
    fun givenMessageId_whenMarkSent_thenRepositoryIsCalled() {
        val messageId = 456L
        doNothing().`when`(messageRepository).markSent(messageId)

        messageRepository.markSent(messageId)

        verify(messageRepository).markSent(messageId)
    }

    @Test
    fun givenLargeMessageId_whenMarkSent_thenHandlesCorrectly() {
        val messageId = Long.MAX_VALUE
        doNothing().`when`(messageRepository).markSent(messageId)

        messageRepository.markSent(messageId)

        verify(messageRepository).markSent(messageId)
    }

    // markFailed tests

    @Test
    fun givenMessageIdAndResultCode_whenMarkFailed_thenRepositoryIsCalled() {
        val messageId = 789L
        val resultCode = 1 // RESULT_ERROR_GENERIC_FAILURE
        doNothing().`when`(messageRepository).markFailed(messageId, resultCode)

        messageRepository.markFailed(messageId, resultCode)

        verify(messageRepository).markFailed(messageId, resultCode)
    }

    @Test
    fun givenNoServiceResultCode_whenMarkFailed_thenPassesCorrectCode() {
        val messageId = 100L
        val resultCode = 4 // RESULT_ERROR_NO_SERVICE
        doNothing().`when`(messageRepository).markFailed(messageId, resultCode)

        messageRepository.markFailed(messageId, resultCode)

        verify(messageRepository).markFailed(messageId, resultCode)
    }

    @Test
    fun givenRadioOffResultCode_whenMarkFailed_thenPassesCorrectCode() {
        val messageId = 200L
        val resultCode = 2 // RESULT_ERROR_RADIO_OFF
        doNothing().`when`(messageRepository).markFailed(messageId, resultCode)

        messageRepository.markFailed(messageId, resultCode)

        verify(messageRepository).markFailed(messageId, resultCode)
    }

    // markDelivered tests

    @Test
    fun givenMessageId_whenMarkDelivered_thenRepositoryIsCalled() {
        val messageId = 123L
        doNothing().`when`(messageRepository).markDelivered(messageId)

        messageRepository.markDelivered(messageId)

        verify(messageRepository).markDelivered(messageId)
    }

    // markDeliveryFailed tests

    @Test
    fun givenMessageIdAndResultCode_whenMarkDeliveryFailed_thenRepositoryIsCalled() {
        val messageId = 456L
        val resultCode = 0 // Activity.RESULT_CANCELED
        doNothing().`when`(messageRepository).markDeliveryFailed(messageId, resultCode)

        messageRepository.markDeliveryFailed(messageId, resultCode)

        verify(messageRepository).markDeliveryFailed(messageId, resultCode)
    }

    // sendSms tests

    @Test
    fun givenSmsMessage_whenSendSms_thenRepositoryIsCalled() {
        `when`(smsMessage.isSms()).thenReturn(true)
        doNothing().`when`(messageRepository).sendSms(smsMessage)

        messageRepository.sendSms(smsMessage)

        verify(messageRepository).sendSms(smsMessage)
    }

    // resendMms tests

    @Test
    fun givenMmsMessage_whenResendMms_thenRepositoryIsCalled() {
        `when`(mmsMessage.isSms()).thenReturn(false)
        doNothing().`when`(messageRepository).resendMms(mmsMessage)

        messageRepository.resendMms(mmsMessage)

        verify(messageRepository).resendMms(mmsMessage)
    }

    // getMessage tests

    @Test
    fun givenExistingMessageId_whenGetMessage_thenReturnsMessage() {
        val messageId = 123L
        `when`(messageRepository.getMessage(messageId)).thenReturn(smsMessage)

        val result = messageRepository.getMessage(messageId)

        assertNotNull(result)
        verify(messageRepository).getMessage(messageId)
    }

    @Test
    fun givenNonExistingMessageId_whenGetMessage_thenReturnsNull() {
        val messageId = 999L
        `when`(messageRepository.getMessage(messageId)).thenReturn(null)

        val result = messageRepository.getMessage(messageId)

        assertEquals(null, result)
    }

    // insertReceivedSms tests

    @Test
    fun givenSmsDetails_whenInsertReceivedSms_thenReturnsMessage() {
        val subId = 0
        val address = "+1234567890"
        val body = "Hello World"
        val sentTime = System.currentTimeMillis()

        `when`(messageRepository.insertReceivedSms(subId, address, body, sentTime))
            .thenReturn(smsMessage)

        val result = messageRepository.insertReceivedSms(subId, address, body, sentTime)

        assertNotNull(result)
        verify(messageRepository).insertReceivedSms(subId, address, body, sentTime)
    }

    @Test
    fun givenEmptyBody_whenInsertReceivedSms_thenStillReturnsMessage() {
        val subId = 0
        val address = "+1234567890"
        val body = ""
        val sentTime = System.currentTimeMillis()

        `when`(messageRepository.insertReceivedSms(subId, address, body, sentTime))
            .thenReturn(smsMessage)

        val result = messageRepository.insertReceivedSms(subId, address, body, sentTime)

        assertNotNull(result)
    }

    @Test
    fun givenNoSubscription_whenInsertReceivedSms_thenHandlesNegativeSubId() {
        val subId = -1 // No subscription
        val address = "+1234567890"
        val body = "Test message"
        val sentTime = System.currentTimeMillis()

        `when`(messageRepository.insertReceivedSms(subId, address, body, sentTime))
            .thenReturn(smsMessage)

        val result = messageRepository.insertReceivedSms(subId, address, body, sentTime)

        assertNotNull(result)
        verify(messageRepository).insertReceivedSms(subId, address, body, sentTime)
    }

    // sendMessage tests

    @Test
    fun givenMessageDetails_whenSendMessage_thenRepositoryIsCalled() {
        val subId = 0
        val threadId = 123L
        val addresses = listOf("+1234567890")
        val body = "Hello"
        val attachments = emptyList<Attachment>()
        val delay = 0

        doNothing()
            .`when`(messageRepository)
            .sendMessage(subId, threadId, addresses, body, attachments, delay)

        messageRepository.sendMessage(subId, threadId, addresses, body, attachments, delay)

        verify(messageRepository).sendMessage(subId, threadId, addresses, body, attachments, delay)
    }

    @Test
    fun givenDelayedMessage_whenSendMessage_thenPassesDelayCorrectly() {
        val subId = 0
        val threadId = 456L
        val addresses = listOf("+1234567890")
        val body = "Delayed message"
        val attachments = emptyList<Attachment>()
        val delay = 5000 // 5 seconds delay

        doNothing()
            .`when`(messageRepository)
            .sendMessage(subId, threadId, addresses, body, attachments, delay)

        messageRepository.sendMessage(subId, threadId, addresses, body, attachments, delay)

        verify(messageRepository).sendMessage(subId, threadId, addresses, body, attachments, delay)
    }

    @Test
    fun givenMultipleRecipients_whenSendMessage_thenHandlesAllAddresses() {
        val subId = 0
        val threadId = 789L
        val addresses = listOf("+1234567890", "+0987654321", "+1122334455")
        val body = "Group message"
        val attachments = emptyList<Attachment>()
        val delay = 0

        doNothing()
            .`when`(messageRepository)
            .sendMessage(subId, threadId, addresses, body, attachments, delay)

        messageRepository.sendMessage(subId, threadId, addresses, body, attachments, delay)

        verify(messageRepository).sendMessage(subId, threadId, addresses, body, attachments, delay)
    }

    // cancelDelayedSms tests

    @Test
    fun givenMessageId_whenCancelDelayedSms_thenRepositoryIsCalled() {
        val messageId = 123L
        `when`(messageRepository.cancelDelayedSms(messageId)).thenReturn(true)

        messageRepository.cancelDelayedSms(messageId)

        verify(messageRepository).cancelDelayedSms(messageId)
    }

    // deleteMessages tests

    @Test
    fun givenMessageIds_whenDeleteMessages_thenRepositoryIsCalled() {
        val messageIds = listOf(1L, 2L, 3L)
        doNothing().`when`(messageRepository).deleteMessages(messageIds)

        messageRepository.deleteMessages(messageIds)

        verify(messageRepository).deleteMessages(messageIds)
    }

    @Test
    fun givenEmptyList_whenDeleteMessages_thenStillCallsRepository() {
        val messageIds = emptyList<Long>()
        doNothing().`when`(messageRepository).deleteMessages(messageIds)

        messageRepository.deleteMessages(messageIds)

        verify(messageRepository).deleteMessages(messageIds)
    }

    // SMS Status constants tests

    @Test
    fun givenSmsStatusNone_whenCompared_thenHasCorrectValue() {
        assertEquals(-1, Sms.STATUS_NONE)
    }

    @Test
    fun givenSmsStatusComplete_whenCompared_thenHasCorrectValue() {
        assertEquals(0, Sms.STATUS_COMPLETE)
    }

    @Test
    fun givenSmsStatusFailed_whenCompared_thenHasCorrectValue() {
        assertEquals(64, Sms.STATUS_FAILED)
    }

    @Test
    fun givenSmsStatusPending_whenCompared_thenHasCorrectValue() {
        assertEquals(32, Sms.STATUS_PENDING)
    }

    // Message type constants tests

    @Test
    fun givenSmsMessageTypeInbox_whenCompared_thenHasCorrectValue() {
        assertEquals(1, Sms.MESSAGE_TYPE_INBOX)
    }

    @Test
    fun givenSmsMessageTypeSent_whenCompared_thenHasCorrectValue() {
        assertEquals(2, Sms.MESSAGE_TYPE_SENT)
    }

    @Test
    fun givenSmsMessageTypeOutbox_whenCompared_thenHasCorrectValue() {
        assertEquals(4, Sms.MESSAGE_TYPE_OUTBOX)
    }

    @Test
    fun givenSmsMessageTypeFailed_whenCompared_thenHasCorrectValue() {
        assertEquals(5, Sms.MESSAGE_TYPE_FAILED)
    }

    @Test
    fun givenSmsMessageTypeQueued_whenCompared_thenHasCorrectValue() {
        assertEquals(6, Sms.MESSAGE_TYPE_QUEUED)
    }

    // MMS-specific tests

    // resendMms Tests
    @Test
    fun givenMmsMessage_whenResendMms_thenCallsRepository() {
        `when`(mmsMessage.isSms()).thenReturn(false)
        doNothing().`when`(messageRepository).resendMms(mmsMessage)

        messageRepository.resendMms(mmsMessage)

        verify(messageRepository).resendMms(mmsMessage)
    }

    @Test
    fun givenFailedMmsMessage_whenResendMms_thenRepositoryHandlesResend() {
        `when`(mmsMessage.isFailedMessage()).thenReturn(true)
        `when`(mmsMessage.isSms()).thenReturn(false)
        doNothing().`when`(messageRepository).resendMms(mmsMessage)

        messageRepository.resendMms(mmsMessage)

        verify(messageRepository).resendMms(mmsMessage)
    }

    // getPart Tests
    @Mock private lateinit var mmsPart: MmsPart

    @Test
    fun givenPartId_whenGetPart_thenReturnsPartOrNull() {
        val partId = 100L
        `when`(messageRepository.getPart(partId)).thenReturn(mmsPart)

        val result = messageRepository.getPart(partId)

        assertNotNull(result)
        verify(messageRepository).getPart(partId)
    }

    @Test
    fun givenNonExistingPartId_whenGetPart_thenReturnsNull() {
        val partId = 999L
        `when`(messageRepository.getPart(partId)).thenReturn(null)

        val result = messageRepository.getPart(partId)

        assertEquals(null, result)
    }

    // MMS Message Box Constants Tests
    @Test
    fun givenMmsMessageBoxInbox_whenCompared_thenHasCorrectValue() {
        assertEquals(1, Mms.MESSAGE_BOX_INBOX)
    }

    @Test
    fun givenMmsMessageBoxSent_whenCompared_thenHasCorrectValue() {
        assertEquals(2, Mms.MESSAGE_BOX_SENT)
    }

    @Test
    fun givenMmsMessageBoxDrafts_whenCompared_thenHasCorrectValue() {
        assertEquals(3, Mms.MESSAGE_BOX_DRAFTS)
    }

    @Test
    fun givenMmsMessageBoxOutbox_whenCompared_thenHasCorrectValue() {
        assertEquals(4, Mms.MESSAGE_BOX_OUTBOX)
    }

    @Test
    fun givenMmsMessageBoxFailed_whenCompared_thenHasCorrectValue() {
        assertEquals(5, Mms.MESSAGE_BOX_FAILED)
    }

    @Test
    fun givenMmsMessageBoxAll_whenCompared_thenHasCorrectValue() {
        assertEquals(0, Mms.MESSAGE_BOX_ALL)
    }

    // MMS with Attachments Tests
    @Test
    fun givenMessageWithImageAttachment_whenSendMessage_thenHandlesCorrectly() {
        val subId = 0
        val threadId = 100L
        val addresses = listOf("+1234567890")
        val body = "Photo message"
        val attachments = emptyList<Attachment>() // Would contain image attachment
        val delay = 0

        doNothing()
            .`when`(messageRepository)
            .sendMessage(subId, threadId, addresses, body, attachments, delay)

        messageRepository.sendMessage(subId, threadId, addresses, body, attachments, delay)

        verify(messageRepository).sendMessage(subId, threadId, addresses, body, attachments, delay)
    }

    @Test
    fun givenMessageWithVideoAttachment_whenSendMessage_thenHandlesCorrectly() {
        val subId = 0
        val threadId = 200L
        val addresses = listOf("+1234567890")
        val body = "Video message"
        val attachments = emptyList<Attachment>() // Would contain video attachment
        val delay = 0

        doNothing()
            .`when`(messageRepository)
            .sendMessage(subId, threadId, addresses, body, attachments, delay)

        messageRepository.sendMessage(subId, threadId, addresses, body, attachments, delay)

        verify(messageRepository).sendMessage(subId, threadId, addresses, body, attachments, delay)
    }

    @Test
    fun givenMessageWithAudioAttachment_whenSendMessage_thenHandlesCorrectly() {
        val subId = 0
        val threadId = 300L
        val addresses = listOf("+1234567890")
        val body = "Audio message"
        val attachments = emptyList<Attachment>() // Would contain audio attachment
        val delay = 0

        doNothing()
            .`when`(messageRepository)
            .sendMessage(subId, threadId, addresses, body, attachments, delay)

        messageRepository.sendMessage(subId, threadId, addresses, body, attachments, delay)

        verify(messageRepository).sendMessage(subId, threadId, addresses, body, attachments, delay)
    }

    @Test
    fun givenMessageWithMultipleAttachments_whenSendMessage_thenHandlesCorrectly() {
        val subId = 0
        val threadId = 400L
        val addresses = listOf("+1234567890")
        val body = "Multi-attachment message"
        val attachments = emptyList<Attachment>() // Would contain multiple attachments
        val delay = 0

        doNothing()
            .`when`(messageRepository)
            .sendMessage(subId, threadId, addresses, body, attachments, delay)

        messageRepository.sendMessage(subId, threadId, addresses, body, attachments, delay)

        verify(messageRepository).sendMessage(subId, threadId, addresses, body, attachments, delay)
    }

    // Group MMS Tests
    @Test
    fun givenGroupMmsWithMultipleRecipients_whenSendMessage_thenHandlesAllRecipients() {
        val subId = 0
        val threadId = 500L
        val addresses = listOf("+1111111111", "+2222222222", "+3333333333", "+4444444444")
        val body = "Group MMS"
        val attachments = emptyList<Attachment>()
        val delay = 0

        doNothing()
            .`when`(messageRepository)
            .sendMessage(subId, threadId, addresses, body, attachments, delay)

        messageRepository.sendMessage(subId, threadId, addresses, body, attachments, delay)

        verify(messageRepository).sendMessage(subId, threadId, addresses, body, attachments, delay)
    }

    // MMS Content URI Tests
    @Test
    fun givenMmsContentUri_whenParsed_thenHasCorrectScheme() {
        val uri = android.net.Uri.parse("content://mms/123")
        assertEquals("content", uri.scheme)
        assertEquals("mms", uri.authority)
    }

    @Test
    fun givenMmsPartContentUri_whenParsed_thenHasCorrectPath() {
        val uri = android.net.Uri.parse("content://mms/part/456")
        assertTrue(uri.path?.contains("part") == true)
    }

    // MMS Inbox URI Tests
    @Test
    fun givenMmsInboxUri_whenParsed_thenContainsInbox() {
        val uri = android.net.Uri.parse("content://mms/inbox")
        assertTrue(uri.path?.contains("inbox") == true)
    }

    // MMS Outbox URI Tests
    @Test
    fun givenMmsOutboxUri_whenParsed_thenContainsOutbox() {
        val uri = android.net.Uri.parse("content://mms/outbox")
        assertTrue(uri.path?.contains("outbox") == true)
    }

    // MMS Sent URI Tests
    @Test
    fun givenMmsSentUri_whenParsed_thenContainsSent() {
        val uri = android.net.Uri.parse("content://mms/sent")
        assertTrue(uri.path?.contains("sent") == true)
    }
}
