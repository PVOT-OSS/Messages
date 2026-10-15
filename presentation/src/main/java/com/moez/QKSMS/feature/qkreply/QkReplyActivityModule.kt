/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.qkreply

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Named
import org.prauga.messages.injection.ViewModelKey

@Module
class QkReplyActivityModule {

    @Provides
    @Named("threadId")
    fun provideThreadId(activity: QkReplyActivity): Long =
        activity.intent.extras?.getLong("threadId") ?: 0L

    @Provides
    @IntoMap
    @ViewModelKey(QkReplyViewModel::class)
    fun provideQkReplyViewModel(viewModel: QkReplyViewModel): ViewModel = viewModel
}
