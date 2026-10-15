/*
 * SPDX-FileCopyrightText: 2007 Esmertec AG.
 * SPDX-FileCopyrightText: 2007 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.mms.dom.smil;

import org.w3c.dom.smil.Time;
import org.w3c.dom.smil.TimeList;

import java.util.ArrayList;

public class TimeListImpl implements TimeList {
    private final ArrayList<Time> mTimes;

    /*
     * Internal Interface
     */
    TimeListImpl(ArrayList<Time> times) {
        mTimes = times;
    }

    /*
     * TimeList Interface
     */

    public int getLength() {
        return mTimes.size();
    }

    public Time item(int index) {
        Time time = null;
        try {
            time = mTimes.get(index);
        } catch (IndexOutOfBoundsException e) {
            // Do nothing and return null
        }
        return time;
    }

}
