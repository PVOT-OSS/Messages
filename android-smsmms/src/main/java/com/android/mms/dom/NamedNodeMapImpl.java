/*
 * SPDX-FileCopyrightText: 2007 Esmertec AG.
 * SPDX-FileCopyrightText: 2007 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.mms.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Vector;

public class NamedNodeMapImpl implements NamedNodeMap {

    private Vector<Node> mNodes = new Vector<Node>();

    public int getLength() {
        return mNodes.size();
    }

    public Node getNamedItem(String name) {
        Node node = null;
        for (int i = 0; i < mNodes.size(); i++) {
            if (name.equals(mNodes.elementAt(i).getNodeName())) {
                node = mNodes.elementAt(i);
                break;
            }
        }
        return node;
    }

    public Node getNamedItemNS(String namespaceURI, String localName) {
        // TODO Auto-generated method stub
        return null;
    }

    public Node item(int index) {
        if (index < mNodes.size()) {
            return mNodes.elementAt(index);
        }
        return null;
    }

    public Node removeNamedItem(String name) throws DOMException {
        Node node = getNamedItem(name);
        if (node == null) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "Not found");
        } else {
            mNodes.remove(node);
        }
        return node;
    }

    public Node removeNamedItemNS(String namespaceURI, String localName)
            throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    public Node setNamedItem(Node arg) throws DOMException {
        Node existing = getNamedItem(arg.getNodeName());
        if (existing != null) {
            mNodes.remove(existing);
        }
        mNodes.add(arg);
        return existing;
    }

    public Node setNamedItemNS(Node arg) throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

}
