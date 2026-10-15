/*
 * SPDX-FileCopyrightText: 2007 Esmertec AG.
 * SPDX-FileCopyrightText: 2007 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.mms.dom.smil;

import com.android.mms.dom.ElementImpl;
import org.w3c.dom.DOMException;
import org.w3c.dom.smil.SMILElement;

public class SmilElementImpl extends ElementImpl implements SMILElement {
    /**
     * This constructor is used by the factory methods of the SmilDocument.
     *
     * @param owner The SMIL document to which this element belongs to
     * @param tagName The tag name of the element
     */
    SmilElementImpl(SmilDocumentImpl owner, String tagName)
    {
        super(owner, tagName.toLowerCase());
    }

    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setId(String id) throws DOMException {
        // TODO Auto-generated method stub

    }

}
