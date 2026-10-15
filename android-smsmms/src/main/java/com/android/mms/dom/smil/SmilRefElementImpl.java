/*
 * SPDX-FileCopyrightText: 2007 Esmertec AG.
 * SPDX-FileCopyrightText: 2007 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.mms.dom.smil;

import org.w3c.dom.smil.SMILRefElement;

public class SmilRefElementImpl extends SmilRegionMediaElementImpl implements
        SMILRefElement {

    SmilRefElementImpl(SmilDocumentImpl owner, String tagName) {
        super(owner, tagName);
    }

}
