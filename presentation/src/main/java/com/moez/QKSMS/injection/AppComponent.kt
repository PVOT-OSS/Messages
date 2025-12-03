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
package org.prauga.messages.injection

import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import org.prauga.messages.common.QKApplication
import org.prauga.messages.common.QkDialog
import org.prauga.messages.common.util.QkChooserTargetService
import org.prauga.messages.common.widget.AvatarView
import org.prauga.messages.common.widget.PagerTitleView
import org.prauga.messages.common.widget.PreferenceView
import org.prauga.messages.common.widget.QkEditText
import org.prauga.messages.common.widget.QkSwitch
import org.prauga.messages.common.widget.QkTextView
import org.prauga.messages.common.widget.RadioPreferenceView
import org.prauga.messages.feature.backup.BackupController
import org.prauga.messages.feature.blocking.BlockingController
import org.prauga.messages.feature.blocking.filters.MessageContentFiltersController
import org.prauga.messages.feature.blocking.manager.BlockingManagerController
import org.prauga.messages.feature.blocking.messages.BlockedMessagesController
import org.prauga.messages.feature.blocking.numbers.BlockedNumbersController
import org.prauga.messages.feature.compose.editing.DetailedChipView
import org.prauga.messages.feature.conversationinfo.injection.ConversationInfoComponent
import org.prauga.messages.feature.settings.SettingsController
import org.prauga.messages.feature.settings.about.AboutController
import org.prauga.messages.feature.settings.swipe.SwipeActionsController
import org.prauga.messages.feature.themepicker.injection.ThemePickerComponent
import org.prauga.messages.feature.widget.WidgetAdapter
import org.prauga.messages.injection.android.ActivityBuilderModule
import org.prauga.messages.injection.android.BroadcastReceiverBuilderModule
import org.prauga.messages.injection.android.ServiceBuilderModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AndroidSupportInjectionModule::class,
    AppModule::class,
    ActivityBuilderModule::class,
    BroadcastReceiverBuilderModule::class,
    ServiceBuilderModule::class])
interface AppComponent {

    fun conversationInfoBuilder(): ConversationInfoComponent.Builder
    fun themePickerBuilder(): ThemePickerComponent.Builder

    fun inject(application: QKApplication)

    fun inject(controller: AboutController)
    fun inject(controller: BackupController)
    fun inject(controller: BlockedMessagesController)
    fun inject(controller: BlockedNumbersController)
    fun inject(controller: MessageContentFiltersController)
    fun inject(controller: BlockingController)
    fun inject(controller: BlockingManagerController)
    fun inject(controller: SettingsController)
    fun inject(controller: SwipeActionsController)

    fun inject(dialog: QkDialog)

    fun inject(service: WidgetAdapter)

    /**
     * This can't use AndroidInjection, or else it will crash on pre-marshmallow devices
     */
    fun inject(service: QkChooserTargetService)

    fun inject(view: AvatarView)
    fun inject(view: DetailedChipView)
    fun inject(view: PagerTitleView)
    fun inject(view: PreferenceView)
    fun inject(view: RadioPreferenceView)
    fun inject(view: QkEditText)
    fun inject(view: QkSwitch)
    fun inject(view: QkTextView)

}
