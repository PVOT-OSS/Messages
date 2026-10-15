/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.gallery

import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Named
import org.prauga.messages.injection.ViewModelKey

@Module
class GalleryActivityModule {

    @Provides fun provideIntent(activity: GalleryActivity): Intent = activity.intent

    @Provides @Named("partId") fun providePartId(activity: GalleryActivity): Long = activity.partId

    @Provides
    @IntoMap
    @ViewModelKey(GalleryViewModel::class)
    fun provideGalleryViewModel(viewModel: GalleryViewModel): ViewModel = viewModel
}
