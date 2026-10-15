/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.contacts

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import org.prauga.messages.injection.ViewModelKey

@Module
class ContactsActivityModule {

    @Provides
    fun provideIsSharing(activity: ContactsActivity): Boolean {
        return activity.intent.extras?.getBoolean(ContactsActivity.SharingKey, false) ?: false
    }

    @Provides
    fun provideChips(activity: ContactsActivity): HashMap<String, String?> {
        return activity.intent.extras?.getSerializable(ContactsActivity.ChipsKey)?.let {
            serializable ->
            serializable as? HashMap<String, String?>
        } ?: hashMapOf()
    }

    @Provides
    @IntoMap
    @ViewModelKey(ContactsViewModel::class)
    fun provideContactsViewModel(viewModel: ContactsViewModel): ViewModel = viewModel
}
