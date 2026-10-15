/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

import io.reactivex.Observable

interface RatingManager {

    val shouldShowRating: Observable<Boolean>

    /** Whether or not we should show the rating UI should depend on the number of sessions */
    fun addSession()

    fun rate()

    fun dismiss()
}
