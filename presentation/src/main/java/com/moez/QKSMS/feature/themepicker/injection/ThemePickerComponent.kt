/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.themepicker.injection

import dagger.Subcomponent
import org.prauga.messages.feature.themepicker.ThemePickerController
import org.prauga.messages.injection.scope.ControllerScope

@ControllerScope
@Subcomponent(modules = [ThemePickerModule::class])
interface ThemePickerComponent {

    fun inject(controller: ThemePickerController)

    @Subcomponent.Builder
    interface Builder {
        fun themePickerModule(module: ThemePickerModule): Builder

        fun build(): ThemePickerComponent
    }
}
