/*
 * SPDX-FileCopyrightText: 2007 Esmertec AG.
 * SPDX-FileCopyrightText: 2007 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.mms.dom.smil;

import org.w3c.dom.DOMException;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.DocumentEvent;
import org.w3c.dom.events.Event;
import org.w3c.dom.smil.ElementParallelTimeContainer;
import org.w3c.dom.smil.ElementTime;
import org.w3c.dom.smil.SMILParElement;
import org.w3c.dom.smil.Time;
import org.w3c.dom.smil.TimeList;

import java.util.ArrayList;

public class SmilParElementImpl extends SmilElementImpl implements SMILParElement {
    public final static String SMIL_SLIDE_START_EVENT = "SmilSlideStart";
    public final static String SMIL_SLIDE_END_EVENT = "SmilSlideEnd";

    ElementParallelTimeContainer mParTimeContainer =
        new ElementParallelTimeContainerImpl(this) {
        @Override
        public TimeList getBegin() {
            /*
             * For children of a sequence, the only legal value for begin is
             * a (single) non-negative offset value.
             */
            TimeList beginTimeList = super.getBegin();
            if (beginTimeList.getLength() > 1) {
                ArrayList<Time> singleTimeContainer = new ArrayList<Time>();
                singleTimeContainer.add(beginTimeList.item(0));
                beginTimeList = new TimeListImpl(singleTimeContainer);
            }
            return beginTimeList;
        }

        public NodeList getTimeChildren() {
            return getChildNodes();
        }

        public boolean beginElement() {
            DocumentEvent doc = (DocumentEvent) SmilParElementImpl.this.getOwnerDocument();
            Event startEvent = doc.createEvent("Event");
            startEvent.initEvent(SMIL_SLIDE_START_EVENT, false, false);
            dispatchEvent(startEvent);
            return true;
        }

        public boolean endElement() {
            DocumentEvent doc = (DocumentEvent) SmilParElementImpl.this.getOwnerDocument();
            Event endEvent = doc.createEvent("Event");
            endEvent.initEvent(SMIL_SLIDE_END_EVENT, false, false);
            dispatchEvent(endEvent);
            return true;
        }

        public void pauseElement() {
            // TODO Auto-generated method stub

        }

        public void resumeElement() {
            // TODO Auto-generated method stub

        }

        public void seekElement(float seekTo) {
            // TODO Auto-generated method stub

        }

        ElementTime getParentElementTime() {
            return ((SmilDocumentImpl) mSmilElement.getOwnerDocument()).mSeqTimeContainer;
        }
    };

    /*
     * Internal Interface
     */

    SmilParElementImpl(SmilDocumentImpl owner, String tagName)
    {
        super(owner, tagName.toUpperCase());
    }

    int getBeginConstraints() {
        /*
         * For children of a sequence, the only legal value for begin is
         * a (single) non-negative offset value.
         */
        return (TimeImpl.ALLOW_OFFSET_VALUE); // Do not set ALLOW_NEGATIVE_VALUE
    }

    /*
     * ElementParallelTimeContainer
     */

    public String getEndSync() {
        return mParTimeContainer.getEndSync();
    }

    public float getImplicitDuration() {
        return mParTimeContainer.getImplicitDuration();
    }

    public void setEndSync(String endSync) throws DOMException {
        mParTimeContainer.setEndSync(endSync);
    }

    public NodeList getActiveChildrenAt(float instant) {
        return mParTimeContainer.getActiveChildrenAt(instant);
    }

    public NodeList getTimeChildren() {
        return mParTimeContainer.getTimeChildren();
    }

    public boolean beginElement() {
        return mParTimeContainer.beginElement();
    }

    public boolean endElement() {
        return mParTimeContainer.endElement();
    }

    public TimeList getBegin() {
        return mParTimeContainer.getBegin();
    }

    public float getDur() {
        return mParTimeContainer.getDur();
    }

    public TimeList getEnd() {
        return mParTimeContainer.getEnd();
    }

    public short getFill() {
        return mParTimeContainer.getFill();
    }

    public short getFillDefault() {
        return mParTimeContainer.getFillDefault();
    }

    public float getRepeatCount() {
        return mParTimeContainer.getRepeatCount();
    }

    public float getRepeatDur() {
        return mParTimeContainer.getRepeatDur();
    }

    public short getRestart() {
        return mParTimeContainer.getRestart();
    }

    public void pauseElement() {
        mParTimeContainer.pauseElement();
    }

    public void resumeElement() {
        mParTimeContainer.resumeElement();
    }

    public void seekElement(float seekTo) {
        mParTimeContainer.seekElement(seekTo);
    }

    public void setBegin(TimeList begin) throws DOMException {
        mParTimeContainer.setBegin(begin);
    }

    public void setDur(float dur) throws DOMException {
        mParTimeContainer.setDur(dur);
    }

    public void setEnd(TimeList end) throws DOMException {
        mParTimeContainer.setEnd(end);
    }

    public void setFill(short fill) throws DOMException {
        mParTimeContainer.setFill(fill);
    }

    public void setFillDefault(short fillDefault) throws DOMException {
        mParTimeContainer.setFillDefault(fillDefault);
    }

    public void setRepeatCount(float repeatCount) throws DOMException {
        mParTimeContainer.setRepeatCount(repeatCount);
    }

    public void setRepeatDur(float repeatDur) throws DOMException {
        mParTimeContainer.setRepeatDur(repeatDur);
    }

    public void setRestart(short restart) throws DOMException {
        mParTimeContainer.setRestart(restart);
    }
}
