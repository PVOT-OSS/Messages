/*
 * SPDX-FileCopyrightText: 2007 Esmertec AG.
 * SPDX-FileCopyrightText: 2007 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.mms.dom;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

public class NodeListImpl implements NodeList {
    private ArrayList<Node> mSearchNodes;
    private ArrayList<Node> mStaticNodes;
    private Node mRootNode;
    private String mTagName;
    private boolean mDeepSearch;

    /*
     * Internal Interface
     */

    /**
     * Constructs a NodeList by searching for all descendants or the direct
     * children of a root node with a given tag name.
     * @param rootNode The root <code>Node</code> of the search.
     * @param tagName The tag name to be searched for. If null, all descendants
     *              will be returned.
     * @param deep Limit the search to the direct children of rootNode if false,
     *              to all descendants otherwise.
     */
    public NodeListImpl(Node rootNode, String tagName, boolean deepSearch) {
        mRootNode = rootNode;
        mTagName  = tagName;
        mDeepSearch = deepSearch;
    }

    /**
     * Constructs a NodeList for a given static node list.
     * @param nodes The static node list.
     */
    public NodeListImpl(ArrayList<Node> nodes) {
        mStaticNodes = nodes;
    }

    /*
     * NodeListImpl Interface
     */

    public int getLength() {
        if (mStaticNodes == null) {
            fillList(mRootNode);
            return mSearchNodes.size();
        } else {
            return mStaticNodes.size();
        }
    }

    public Node item(int index) {
        Node node = null;
        if (mStaticNodes == null) {
            fillList(mRootNode);
            try {
                node = mSearchNodes.get(index);
            } catch (IndexOutOfBoundsException e) {
                // Do nothing and return null
            }
        } else {
            try {
                node = mStaticNodes.get(index);
            } catch (IndexOutOfBoundsException e) {
                // Do nothing and return null
            }
        }
        return node;
    }

    /**
     * A preorder traversal is done in the following order:
     * <ul>
     *   <li> Visit root.
     *   <li> Traverse children from left to right in preorder.
     * </ul>
     * This method fills the live node list.
     * @param The root of preorder traversal
     * @return The next match
     */
    private void fillList(Node node) {
        // (Re)-initialize the container if this is the start of the search.
        // Visit the root of this iteration otherwise.
        if (node == mRootNode) {
            mSearchNodes = new ArrayList<Node>();
        } else {
            if ((mTagName == null) || node.getNodeName().equals(mTagName)) {
                mSearchNodes.add(node);
            }
        }

        // Descend one generation...
        node = node.getFirstChild();

        // ...and visit in preorder the children if we are in deep search
        // or directly add the children to the list otherwise.
        while (node != null) {
            if (mDeepSearch) {
                fillList(node);
            } else {
                if ((mTagName == null) || node.getNodeName().equals(mTagName)) {
                    mSearchNodes.add(node);
                }
            }
            node = node.getNextSibling();
        }
    }
}
