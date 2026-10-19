/*
 * SPDX-FileCopyrightText: 2015 Jacob Klinker
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.mms.pdu_alt;

import com.google.android.mms.InvalidHeaderValueException;

public class ReadRecInd extends GenericPdu {
    /**
     * Constructor, used when composing a M-ReadRec.ind pdu.
     *
     * @param from the from value
     * @param messageId the message ID value
     * @param mmsVersion current viersion of mms
     * @param readStatus the read status value
     * @param to the to value
     * @throws InvalidHeaderValueException if parameters are invalid.
     *         NullPointerException if messageId or to is null.
     */
    public ReadRecInd(EncodedStringValue from,
                      byte[] messageId,
                      int mmsVersion,
                      int readStatus,
                      EncodedStringValue[] to) throws InvalidHeaderValueException {
        super();
        setMessageType(PduHeaders.MESSAGE_TYPE_READ_REC_IND);
        setFrom(from);
        setMessageId(messageId);
        setMmsVersion(mmsVersion);
        setTo(to);
        setReadStatus(readStatus);
    }

    /**
     * Constructor with given headers.
     *
     * @param headers Headers for this PDU.
     */
    ReadRecInd(PduHeaders headers) {
        super(headers);
    }

    /**
     * Get Date value.
     *
     * @return the value
     */
    public long getDate() {
        return mPduHeaders.getLongInteger(PduHeaders.DATE);
    }

    /**
     * Set Date value.
     *
     * @param value the value
     */
    public void setDate(long value) {
        mPduHeaders.setLongInteger(value, PduHeaders.DATE);
    }

    /**
     * Get Message-ID value.
     *
     * @return the value
     */
    public byte[] getMessageId() {
        return mPduHeaders.getTextString(PduHeaders.MESSAGE_ID);
    }

    /**
     * Set Message-ID value.
     *
     * @param value the value
     * @throws NullPointerException if the value is null.
     */
    public void setMessageId(byte[] value) {
        mPduHeaders.setTextString(value, PduHeaders.MESSAGE_ID);
    }

    /**
     * Get To value.
     *
     * @return the value
     */
    public EncodedStringValue[] getTo() {
        return mPduHeaders.getEncodedStringValues(PduHeaders.TO);
    }

    /**
     * Set To value.
     *
     * @param value the value
     * @throws NullPointerException if the value is null.
     */
    public void setTo(EncodedStringValue[] value) {
        mPduHeaders.setEncodedStringValues(value, PduHeaders.TO);
    }

    /**
     * Get X-MMS-Read-status value.
     *
     * @return the value
     */
    public int getReadStatus() {
        return mPduHeaders.getOctet(PduHeaders.READ_STATUS);
    }

    /**
     * Set X-MMS-Read-status value.
     *
     * @param value the value
     * @throws InvalidHeaderValueException if the value is invalid.
     */
    public void setReadStatus(int value) throws InvalidHeaderValueException {
        mPduHeaders.setOctet(value, PduHeaders.READ_STATUS);
    }

    /*
     * Optional, not supported header fields:
     *
     *     public byte[] getApplicId() {return null;}
     *     public void setApplicId(byte[] value) {}
     *
     *     public byte[] getAuxApplicId() {return null;}
     *     public void getAuxApplicId(byte[] value) {}
     *
     *     public byte[] getReplyApplicId() {return 0x00;}
     *     public void setReplyApplicId(byte[] value) {}
     */
}
