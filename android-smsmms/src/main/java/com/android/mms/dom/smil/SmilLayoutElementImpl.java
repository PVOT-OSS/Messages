/*
 * SPDX-FileCopyrightText: 2007 Esmertec AG.
 * SPDX-FileCopyrightText: 2007 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.mms.dom.smil;

import com.android.mms.layout.LayoutManager;
import org.w3c.dom.NodeList;
import org.w3c.dom.smil.SMILLayoutElement;
import org.w3c.dom.smil.SMILRootLayoutElement;

public class SmilLayoutElementImpl extends SmilElementImpl implements
        SMILLayoutElement {
    SmilLayoutElementImpl(SmilDocumentImpl owner, String tagName) {
        super(owner, tagName);
    }

    public boolean getResolved() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getType() {
        return this.getAttribute("type");
    }

    public NodeList getRegions() {
        return this.getElementsByTagName("region");
    }

    public SMILRootLayoutElement getRootLayout() {
        NodeList childNodes = this.getChildNodes();
        SMILRootLayoutElement rootLayoutNode = null;
        int childrenCount = childNodes.getLength();
        for (int i = 0; i < childrenCount; i++) {
            if (childNodes.item(i).getNodeName().equals("root-layout")) {
                rootLayoutNode = (SMILRootLayoutElement)childNodes.item(i);
            }
        }
        if (null == rootLayoutNode) {
            // root-layout node is not set. Create a default one.
            rootLayoutNode = (SMILRootLayoutElement) getOwnerDocument().createElement("root-layout");
            rootLayoutNode.setWidth(LayoutManager.getInstance().getLayoutParameters().getWidth());
            rootLayoutNode.setHeight(LayoutManager.getInstance().getLayoutParameters().getHeight());
            appendChild(rootLayoutNode);
        }
        return rootLayoutNode;
    }

}
