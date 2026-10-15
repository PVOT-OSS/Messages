/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import io.reactivex.Flowable
import javax.inject.Inject
import org.prauga.messages.manager.ShortcutManager
import org.prauga.messages.manager.WidgetManager

class UpdateBadge
@Inject
constructor(
    private val shortcutManager: ShortcutManager,
    private val widgetManager: WidgetManager,
) : Interactor<Unit>() {

    override fun buildObservable(params: Unit): Flowable<*> {
        return Flowable.just(params)
            .doOnNext { shortcutManager.updateBadge() }
            .doOnNext { widgetManager.sendDatasetChanged() }
    }
}
