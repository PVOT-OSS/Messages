/*
 * SPDX-FileCopyrightText: 2015 Jacob Klinker
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.mms.pdu_alt;

import com.google.android.mms.InvalidHeaderValueException;

/**
 * M-NofifyResp.ind PDU.
 */
public class NotifyRespInd extends GenericPdu {
    /**
     * Constructor, used when composing a M-NotifyResp.ind pdu.
     *
     * @param mmsVersion current version of mms
     * @param transactionId the transaction-id value
     * @param status the status value
     * @throws InvalidHeaderValueException if parameters are invalid.
     *         NullPointerException if transactionId is null.
     *         RuntimeException if an undeclared error occurs.
     */
    public NotifyRespInd(int mmsVersion,
                         byte[] transactionId,
                         int status) throws InvalidHeaderValueException {
        super();
        setMessageType(PduHeaders.MESSAGE_TYPE_NOTIFYRESP_IND);
        setMmsVersion(mmsVersion);
        setTransactionId(transactionId);
        setStatus(status);
    }

    /**
     * Constructor with given headers.
     *
     * @param headers Headers for this PDU.
     */
    NotifyRespInd(PduHeaders headers) {
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
     *         RuntimeException if an undeclared error occurs.
     */
    public void setReportAllowed(int value) throws InvalidHeaderValueException {
        mPduHeaders.setOctet(value, PduHeaders.REPORT_ALLOWED);
    }

    /**
     * Set X-Mms-Status field value.
     *
     * @param value the value
     * @throws InvalidHeaderValueException if the value is invalid.
     *         RuntimeException if an undeclared error occurs.
     */
    public void setStatus(int value) throws InvalidHeaderValueException {
        mPduHeaders.setOctet(value, PduHeaders.STATUS);
    }

    /**
     * GetX-Mms-Status field value.
     *
     * @return the X-Mms-Status value
     */
    public int getStatus() {
        return mPduHeaders.getOctet(PduHeaders.STATUS);
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
     *         RuntimeException if an undeclared error occurs.
     */
    public void setTransactionId(byte[] value) {
            mPduHeaders.setTextString(value, PduHeaders.TRANSACTION_ID);
    }
}
