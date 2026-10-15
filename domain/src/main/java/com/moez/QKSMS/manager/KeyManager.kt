/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

/** Helper class for generating incrementing ids for messages */
interface KeyManager {

    /** Should be called when a new sync is being started */
    fun reset()

    /** Returns a valid ID that can be used to store a new message */
    fun newId(): Long
}
