/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.google.android.mms.pdu_alt

import org.junit.Assert.assertEquals
import org.junit.Test

class PduHeadersTest {

    // Header Field Constants Tests
    @Test
    fun givenBccHeader_thenHasCorrectValue() {
        assertEquals(0x81, PduHeaders.BCC)
    }

    @Test
    fun givenCcHeader_thenHasCorrectValue() {
        assertEquals(0x82, PduHeaders.CC)
    }

    @Test
    fun givenContentLocationHeader_thenHasCorrectValue() {
        assertEquals(0x83, PduHeaders.CONTENT_LOCATION)
    }

    @Test
    fun givenContentTypeHeader_thenHasCorrectValue() {
        assertEquals(0x84, PduHeaders.CONTENT_TYPE)
    }

    @Test
    fun givenDateHeader_thenHasCorrectValue() {
        assertEquals(0x85, PduHeaders.DATE)
    }

    @Test
    fun givenFromHeader_thenHasCorrectValue() {
        assertEquals(0x89, PduHeaders.FROM)
    }

    @Test
    fun givenToHeader_thenHasCorrectValue() {
        assertEquals(0x97, PduHeaders.TO)
    }

    @Test
    fun givenSubjectHeader_thenHasCorrectValue() {
        assertEquals(0x96, PduHeaders.SUBJECT)
    }

    @Test
    fun givenExpiryHeader_thenHasCorrectValue() {
        assertEquals(0x88, PduHeaders.EXPIRY)
    }

    @Test
    fun givenPriorityHeader_thenHasCorrectValue() {
        assertEquals(0x8F, PduHeaders.PRIORITY)
    }

    @Test
    fun givenTransactionIdHeader_thenHasCorrectValue() {
        assertEquals(0x98, PduHeaders.TRANSACTION_ID)
    }

    @Test
    fun givenMessageSizeHeader_thenHasCorrectValue() {
        assertEquals(0x8E, PduHeaders.MESSAGE_SIZE)
    }

    // Message Type Constants Tests
    @Test
    fun givenMessageTypeSendReq_thenHasCorrectValue() {
        assertEquals(0x80, PduHeaders.MESSAGE_TYPE_SEND_REQ)
    }

    @Test
    fun givenMessageTypeSendConf_thenHasCorrectValue() {
        assertEquals(0x81, PduHeaders.MESSAGE_TYPE_SEND_CONF)
    }

    @Test
    fun givenMessageTypeNotificationInd_thenHasCorrectValue() {
        assertEquals(0x82, PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND)
    }

    @Test
    fun givenMessageTypeRetrieveConf_thenHasCorrectValue() {
        assertEquals(0x84, PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF)
    }

    @Test
    fun givenMessageTypeDeliveryInd_thenHasCorrectValue() {
        assertEquals(0x86, PduHeaders.MESSAGE_TYPE_DELIVERY_IND)
    }

    @Test
    fun givenMessageTypeReadRecInd_thenHasCorrectValue() {
        assertEquals(0x87, PduHeaders.MESSAGE_TYPE_READ_REC_IND)
    }

    @Test
    fun givenMessageTypeAcknowledgeInd_thenHasCorrectValue() {
        assertEquals(0x85, PduHeaders.MESSAGE_TYPE_ACKNOWLEDGE_IND)
    }

    // Priority Constants Tests
    @Test
    fun givenPriorityLow_thenHasCorrectValue() {
        assertEquals(0x80, PduHeaders.PRIORITY_LOW)
    }

    @Test
    fun givenPriorityNormal_thenHasCorrectValue() {
        assertEquals(0x81, PduHeaders.PRIORITY_NORMAL)
    }

    @Test
    fun givenPriorityHigh_thenHasCorrectValue() {
        assertEquals(0x82, PduHeaders.PRIORITY_HIGH)
    }

    @Test
    fun givenPriorities_whenCompared_thenCorrectOrder() {
        assertTrue(PduHeaders.PRIORITY_LOW < PduHeaders.PRIORITY_NORMAL)
        assertTrue(PduHeaders.PRIORITY_NORMAL < PduHeaders.PRIORITY_HIGH)
    }

    // Yes/No Constants Tests
    @Test
    fun givenValueYes_thenHasCorrectValue() {
        assertEquals(0x80, PduHeaders.VALUE_YES)
    }

    @Test
    fun givenValueNo_thenHasCorrectValue() {
        assertEquals(0x81, PduHeaders.VALUE_NO)
    }

    // MMS Version Constants Tests
    @Test
    fun givenMmsVersion10_thenHasCorrectValue() {
        assertEquals((1 shl 4) or 0, PduHeaders.MMS_VERSION_1_0)
    }

    @Test
    fun givenMmsVersion11_thenHasCorrectValue() {
        assertEquals((1 shl 4) or 1, PduHeaders.MMS_VERSION_1_1)
    }

    @Test
    fun givenMmsVersion12_thenHasCorrectValue() {
        assertEquals((1 shl 4) or 2, PduHeaders.MMS_VERSION_1_2)
    }

    @Test
    fun givenMmsVersion13_thenHasCorrectValue() {
        assertEquals((1 shl 4) or 3, PduHeaders.MMS_VERSION_1_3)
    }

    @Test
    fun givenCurrentMmsVersion_thenIs12() {
        assertEquals(PduHeaders.MMS_VERSION_1_2, PduHeaders.CURRENT_MMS_VERSION)
    }

    // Response Status Constants Tests
    @Test
    fun givenResponseStatusOk_thenHasCorrectValue() {
        assertEquals(0x80, PduHeaders.RESPONSE_STATUS_OK)
    }

    @Test
    fun givenResponseStatusErrorUnspecified_thenHasCorrectValue() {
        assertEquals(0x81, PduHeaders.RESPONSE_STATUS_ERROR_UNSPECIFIED)
    }

    @Test
    fun givenResponseStatusErrorServiceDenied_thenHasCorrectValue() {
        assertEquals(0x82, PduHeaders.RESPONSE_STATUS_ERROR_SERVICE_DENIED)
    }

    @Test
    fun givenResponseStatusErrorNetworkProblem_thenHasCorrectValue() {
        assertEquals(0x86, PduHeaders.RESPONSE_STATUS_ERROR_NETWORK_PROBLEM)
    }

    @Test
    fun givenResponseStatusErrorPermanentFailure_thenHasCorrectValue() {
        assertEquals(0xE0, PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_FAILURE)
    }

    // Status Constants Tests
    @Test
    fun givenStatusExpired_thenHasCorrectValue() {
        assertEquals(0x80, PduHeaders.STATUS_EXPIRED)
    }

    @Test
    fun givenStatusRetrieved_thenHasCorrectValue() {
        assertEquals(0x81, PduHeaders.STATUS_RETRIEVED)
    }

    @Test
    fun givenStatusRejected_thenHasCorrectValue() {
        assertEquals(0x82, PduHeaders.STATUS_REJECTED)
    }

    @Test
    fun givenStatusDeferred_thenHasCorrectValue() {
        assertEquals(0x83, PduHeaders.STATUS_DEFERRED)
    }

    // Message Class Constants Tests
    @Test
    fun givenMessageClassPersonal_thenHasCorrectValue() {
        assertEquals(0x80, PduHeaders.MESSAGE_CLASS_PERSONAL)
    }

    @Test
    fun givenMessageClassAdvertisement_thenHasCorrectValue() {
        assertEquals(0x81, PduHeaders.MESSAGE_CLASS_ADVERTISEMENT)
    }

    @Test
    fun givenMessageClassInformational_thenHasCorrectValue() {
        assertEquals(0x82, PduHeaders.MESSAGE_CLASS_INFORMATIONAL)
    }

    @Test
    fun givenMessageClassAuto_thenHasCorrectValue() {
        assertEquals(0x83, PduHeaders.MESSAGE_CLASS_AUTO)
    }

    // Message Class String Constants Tests
    @Test
    fun givenMessageClassPersonalStr_thenCorrectString() {
        assertEquals("personal", PduHeaders.MESSAGE_CLASS_PERSONAL_STR)
    }

    @Test
    fun givenMessageClassAdvertisementStr_thenCorrectString() {
        assertEquals("advertisement", PduHeaders.MESSAGE_CLASS_ADVERTISEMENT_STR)
    }

    @Test
    fun givenMessageClassInformationalStr_thenCorrectString() {
        assertEquals("informational", PduHeaders.MESSAGE_CLASS_INFORMATIONAL_STR)
    }

    // Sender Visibility Constants Tests
    @Test
    fun givenSenderVisibilityHide_thenHasCorrectValue() {
        assertEquals(0x80, PduHeaders.SENDER_VISIBILITY_HIDE)
    }

    @Test
    fun givenSenderVisibilityShow_thenHasCorrectValue() {
        assertEquals(0x81, PduHeaders.SENDER_VISIBILITY_SHOW)
    }

    // Read Status Constants Tests
    @Test
    fun givenReadStatusRead_thenHasCorrectValue() {
        assertEquals(0x80, PduHeaders.READ_STATUS_READ)
    }

    @Test
    fun givenReadStatusDeletedWithoutBeingRead_thenHasCorrectValue() {
        assertEquals(0x81, PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ)
    }

    // MM State Constants Tests
    @Test
    fun givenMmStateDraft_thenHasCorrectValue() {
        assertEquals(0x80, PduHeaders.MM_STATE_DRAFT)
    }

    @Test
    fun givenMmStateSent_thenHasCorrectValue() {
        assertEquals(0x81, PduHeaders.MM_STATE_SENT)
    }

    @Test
    fun givenMmStateNew_thenHasCorrectValue() {
        assertEquals(0x82, PduHeaders.MM_STATE_NEW)
    }

    @Test
    fun givenMmStateRetrieved_thenHasCorrectValue() {
        assertEquals(0x83, PduHeaders.MM_STATE_RETRIEVED)
    }

    // Delivery Report Constants Tests
    @Test
    fun givenDeliveryReportHeader_thenHasCorrectValue() {
        assertEquals(0x86, PduHeaders.DELIVERY_REPORT)
    }

    @Test
    fun givenReadReportHeader_thenHasCorrectValue() {
        assertEquals(0x90, PduHeaders.READ_REPORT)
    }

    // From Address Token Tests
    @Test
    fun givenFromAddressPresentToken_thenHasCorrectValue() {
        assertEquals(0x80, PduHeaders.FROM_ADDRESS_PRESENT_TOKEN)
    }

    @Test
    fun givenFromInsertAddressToken_thenHasCorrectValue() {
        assertEquals(0x81, PduHeaders.FROM_INSERT_ADDRESS_TOKEN)
    }

    // All header constants are unique within their category
    @Test
    fun givenMessageTypes_whenCompared_thenAllUnique() {
        val messageTypes =
            listOf(
                PduHeaders.MESSAGE_TYPE_SEND_REQ,
                PduHeaders.MESSAGE_TYPE_SEND_CONF,
                PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND,
                PduHeaders.MESSAGE_TYPE_NOTIFYRESP_IND,
                PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF,
                PduHeaders.MESSAGE_TYPE_ACKNOWLEDGE_IND,
                PduHeaders.MESSAGE_TYPE_DELIVERY_IND,
                PduHeaders.MESSAGE_TYPE_READ_REC_IND,
            )
        assertEquals(messageTypes.size, messageTypes.toSet().size)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
