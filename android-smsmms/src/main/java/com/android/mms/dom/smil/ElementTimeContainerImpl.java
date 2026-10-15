/*
 * SPDX-FileCopyrightText: 2007 Esmertec AG.
 * SPDX-FileCopyrightText: 2007 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.mms.dom.smil;

import org.w3c.dom.smil.ElementTimeContainer;
import org.w3c.dom.smil.SMILElement;

public abstract class ElementTimeContainerImpl extends ElementTimeImpl implements
        ElementTimeContainer {

    /*
     * Internal Interface
     */

    ElementTimeContainerImpl(SMILElement element) {
        super(element);
    }
}
