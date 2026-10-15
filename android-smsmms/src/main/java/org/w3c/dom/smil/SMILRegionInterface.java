/*
 * SPDX-FileCopyrightText: 2000 World Wide Web Consortium (Massachusetts Institute of Technology, Institut National de Recherche en Informatique et en Automatique, Keio University)
 * SPDX-License-Identifier: W3C-19980720
 */

package org.w3c.dom.smil;

/**
 *  Declares rendering surface for an element. See the  region attribute
 * definition .
 */
public interface SMILRegionInterface {
    /**
     */
    public SMILRegionElement getRegion();
    public void setRegion(SMILRegionElement region);

}
