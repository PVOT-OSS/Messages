/*
 * SPDX-FileCopyrightText: 2015 Jacob Klinker
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.mms.pdu_alt;

import com.google.android.mms.InvalidHeaderValueException;

/**
 * M-Delivery.Ind Pdu.
 */
public class DeliveryInd extends GenericPdu {
    /**
     * Empty constructor.
     * Since the Pdu corresponding to this class is constructed
     * by the Proxy-Relay server, this class is only instantiated
     * by the Pdu Parser.
     *
     * @throws InvalidHeaderValueException if error occurs.
     */
    public DeliveryInd() throws InvalidHeaderValueException {
        super();
        setMessageType(PduHeaders.MESSAGE_TYPE_DELIVERY_IND);
    }

    /**
     * Constructor with given headers.
     *
     * @param headers Headers for this PDU.
     */
    DeliveryInd(PduHeaders headers) {
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
     * @param value the value, should not be null
     * @throws NullPointerException if the value is null.
     */
    public void setMessageId(byte[] value) {
        mPduHeaders.setTextString(value, PduHeaders.MESSAGE_ID);
    }

    /**
     * Get Status value.
     *
     * @return the value
     */
    public int getStatus() {
        return mPduHeaders.getOctet(PduHeaders.STATUS);
    }

    /**
     * Set Status value.
     *
     * @param value the value
     * @throws InvalidHeaderValueException if the value is invalid.
     */
    public void setStatus(int value) throws InvalidHeaderValueException {
        mPduHeaders.setOctet(value, PduHeaders.STATUS);
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
     * set To value.
     *
     * @param value the value
     * @throws NullPointerException if the value is null.
     */
    public void setTo(EncodedStringValue[] value) {
        mPduHeaders.setEncodedStringValues(value, PduHeaders.TO);
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
     *
     *     public EncodedStringValue getStatusText() {return null;}
     *     public void setStatusText(EncodedStringValue value) {}
     */
}
