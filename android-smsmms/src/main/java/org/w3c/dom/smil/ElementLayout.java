/*
 * SPDX-FileCopyrightText: 2000 World Wide Web Consortium (Massachusetts Institute of Technology, Institut National de Recherche en Informatique et en Automatique, Keio University)
 * SPDX-License-Identifier: W3C-19980720
 */

package org.w3c.dom.smil;

import org.w3c.dom.DOMException;

/**
 *  This interface is used by SMIL elements root-layout, top-layout and region.
 *
 */
public interface ElementLayout {
    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public String getTitle();
    public void setTitle(String title)
                                      throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public String getBackgroundColor();
    public void setBackgroundColor(String backgroundColor)
                                      throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public int getHeight();
    public void setHeight(int height)
                                      throws DOMException;

    /**
     * @exception DOMException
     *    NO_MODIFICATION_ALLOWED_ERR: Raised if this attribute is readonly.
     */
    public int getWidth();
    public void setWidth(int width)
                                      throws DOMException;

}
