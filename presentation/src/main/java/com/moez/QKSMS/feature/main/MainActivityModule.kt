/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.main

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.reactivex.disposables.CompositeDisposable
import org.prauga.messages.injection.ViewModelKey
import org.prauga.messages.injection.scope.ActivityScope

@Module
class MainActivityModule {

    @Provides
    @ActivityScope
    fun provideCompositeDiposableLifecycle(): CompositeDisposable = CompositeDisposable()

    @Provides
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    fun provideMainViewModel(viewModel: MainViewModel): ViewModel = viewModel
}
