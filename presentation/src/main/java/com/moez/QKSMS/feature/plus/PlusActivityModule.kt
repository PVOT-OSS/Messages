/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.plus

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import org.prauga.messages.injection.ViewModelKey

@Module
class PlusActivityModule {

    @Provides
    @IntoMap
    @ViewModelKey(PlusViewModel::class)
    fun providePlusViewModel(viewModel: PlusViewModel): ViewModel = viewModel
}
