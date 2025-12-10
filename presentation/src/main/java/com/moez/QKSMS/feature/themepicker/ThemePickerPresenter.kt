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
package org.prauga.messages.feature.themepicker

import com.f2prateek.rx.preferences2.Preference
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.rxkotlin.Observables
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkPresenter
import org.prauga.messages.common.util.Colors
import org.prauga.messages.manager.BillingManager
import org.prauga.messages.manager.WidgetManager
import org.prauga.messages.util.Preferences
import javax.inject.Inject
import javax.inject.Named

class ThemePickerPresenter @Inject constructor(
    prefs: Preferences,
    @Named("recipientId") private val recipientId: Long,
    private val billingManager: BillingManager,
    private val colors: Colors,
    private val navigator: Navigator,
    private val widgetManager: WidgetManager
) : QkPresenter<ThemePickerView, ThemePickerState>(ThemePickerState(recipientId = recipientId)) {

    private val theme: Preference<Int> = prefs.theme(recipientId)

    override fun bindIntents(view: ThemePickerView) {
        super.bindIntents(view)

        theme.asObservable()
            .autoDispose(view.scope())
            .subscribe { color -> view.setCurrentTheme(color) }

        // Update the theme when a material theme is clicked
        view.themeSelected()
            .autoDispose(view.scope())
            .subscribe { color ->
                theme.set(color)
                if (recipientId == 0L) {
                    widgetManager.updateTheme()
                }
            }

        // Update the color of the apply button
        view.hsvThemeSelected()
            .doOnNext { color -> newState { copy(newColor = color) } }
            .map { color -> colors.textPrimaryOnThemeForColor(color) }
            .doOnNext { color -> newState { copy(newTextColor = color) } }
            .autoDispose(view.scope())
            .subscribe()

        // Toggle the visibility of the apply group
        Observables.combineLatest(
            theme.asObservable(),
            view.hsvThemeSelected()
        ) { old, new -> old != new }
            .autoDispose(view.scope())
            .subscribe { themeChanged -> newState { copy(applyThemeVisible = themeChanged) } }

        // Update the theme, when apply is clicked
        view.applyHsvThemeClicks()
            .withLatestFrom(view.hsvThemeSelected()) { _, color -> color }
            .withLatestFrom(billingManager.upgradeStatus) { color, upgraded ->
                if (!upgraded) {
                    view.showQksmsPlusSnackbar()
                } else {
                    theme.set(color)
                    if (recipientId == 0L) {
                        widgetManager.updateTheme()
                    }
                }
            }
            .autoDispose(view.scope())
            .subscribe()

        // Show QKSMS+ activity
        view.viewQksmsPlusClicks()
            .autoDispose(view.scope())
            .subscribe { navigator.showQksmsPlusActivity("settings_theme") }

        // Reset the theme
        view.clearHsvThemeClicks()
            .withLatestFrom(theme.asObservable()) { _, color -> color }
            .autoDispose(view.scope())
            .subscribe { color -> view.setCurrentTheme(color) }
    }

}