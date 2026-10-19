/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.settings.about

import io.reactivex.Observable
import org.prauga.messages.common.base.QkViewContract
import org.prauga.messages.common.widget.PreferenceView

interface AboutView : QkViewContract<Unit> {

    fun preferenceClicks(): Observable<PreferenceView>
}
