/*
 * SPDX-FileCopyrightText: 2015 Jacob Klinker
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.mms.pdu_alt;

import com.google.android.mms.InvalidHeaderValueException;

/**
 * M-Acknowledge.ind PDU.
 */
public class AcknowledgeInd extends GenericPdu {
    /**
     * Constructor, used when composing a M-Acknowledge.ind pdu.
     *
     * @param mmsVersion current viersion of mms
     * @param transactionId the transaction-id value
     * @throws InvalidHeaderValueException if parameters are invalid.
     *         NullPointerException if transactionId is null.
     */
    public AcknowledgeInd(int mmsVersion, byte[] transactionId)
            throws InvalidHeaderValueException {
        super();

        setMessageType(PduHeaders.MESSAGE_TYPE_ACKNOWLEDGE_IND);
        setMmsVersion(mmsVersion);
        setTransactionId(transactionId);
    }

    /**
     * Constructor with given headers.
     *
     * @param headers Headers for this PDU.
     */
    AcknowledgeInd(PduHeaders headers) {
        super(headers);
    }

    /**
     * Get X-Mms-Report-Allowed field value.
     *
     * @return the X-Mms-Report-Allowed value
     */
    public int getReportAllowed() {
        return mPduHeaders.getOctet(PduHeaders.REPORT_ALLOWED);
    }

    /**
     * Set X-Mms-Report-Allowed field value.
     *
     * @param value the value
     * @throws InvalidHeaderValueException if the value is invalid.
     */
    public void setReportAllowed(int value) throws InvalidHeaderValueException {
        mPduHeaders.setOctet(value, PduHeaders.REPORT_ALLOWED);
    }

    /**
     * Get X-Mms-Transaction-Id field value.
     *
     * @return the X-Mms-Report-Allowed value
     */
    public byte[] getTransactionId() {
        return mPduHeaders.getTextString(PduHeaders.TRANSACTION_ID);
    }

    /**
     * Set X-Mms-Transaction-Id field value.
     *
     * @param value the value
     * @throws NullPointerException if the value is null.
     */
    public void setTransactionId(byte[] value) {
        mPduHeaders.setTextString(value, PduHeaders.TRANSACTION_ID);
    }
}
