/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.scheduled

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Named
import org.prauga.messages.injection.ViewModelKey

@Module
class ScheduledActivityModule {

    @Provides
    @IntoMap
    @ViewModelKey(ScheduledViewModel::class)
    fun provideScheduledViewModel(viewModel: ScheduledViewModel): ViewModel = viewModel

    @Provides
    @Named("conversationId")
    fun getConversationId(activity: ScheduledActivity): Long? =
        activity.intent.extras?.getLong("conversationId")
}
