/*
 * SPDX-FileCopyrightText: 2000 World Wide Web Consortium (Massachusetts Institute of Technology, Institut National de Recherche en Informatique et en Automatique, Keio University)
 * SPDX-License-Identifier: W3C-19980720
 */

package org.w3c.dom.smil;

import org.w3c.dom.DOMException;

/**
 *  Controls the position, size and scaling of media object elements. See the
 * region element definition .
 */
public interface SMILRegionElement extends SMILElement, ElementLayout {
    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public String getFit();
    public void setFit(String fit)
                                      throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public int getLeft();
    public void setLeft(int top)
                                      throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public int getTop();
    public void setTop(int top)
                                      throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public int getZIndex();
    public void setZIndex(int zIndex)
                                      throws DOMException;

}
