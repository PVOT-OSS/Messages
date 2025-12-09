/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.prauga.messages.mapper

import io.reactivex.rxjava3.kotlin.Observables
import org.prauga.messages.manager.RatingManager
import org.prauga.messages.util.Preferences
import javax.inject.Inject

class RatingManagerImpl @Inject constructor(
    private val prefs: Preferences,
) : RatingManager {

    companion object {
        private const val RATING_THRESHOLD = 10
    }

    private val sessions = prefs.preferential("sessions", 0)
    private val rated = prefs.preferential("rated", false)
    private val dismissed = prefs.preferential("dismissed", false)

    override val shouldShowRating = Observables.combineLatest(
            sessions.asObservable(),
            rated.asObservable(),
            dismissed.asObservable()
    ) { sessions, rated, dismissed ->
        sessions > RATING_THRESHOLD && !rated && !dismissed
    }

    override fun addSession() {
        sessions.set(sessions.get() + 1)
    }

    override fun rate() {
        rated.set(true)
    }

    override fun dismiss() {
        dismissed.set(true)
    }

}
