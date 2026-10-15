/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.themepicker.injection

import dagger.Module
import dagger.Provides
import javax.inject.Named
import org.prauga.messages.feature.themepicker.ThemePickerController
import org.prauga.messages.injection.scope.ControllerScope

@Module
class ThemePickerModule(private val controller: ThemePickerController) {

    @Provides
    @ControllerScope
    @Named("recipientId")
    fun provideThreadId(): Long = controller.recipientId
}
