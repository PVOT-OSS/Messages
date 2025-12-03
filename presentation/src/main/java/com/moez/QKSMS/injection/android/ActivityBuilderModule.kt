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
package org.prauga.messages.injection.android

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.prauga.messages.feature.backup.BackupActivity
import org.prauga.messages.feature.blocking.BlockingActivity
import org.prauga.messages.feature.compose.ComposeActivity
import org.prauga.messages.feature.compose.ComposeActivityModule
import org.prauga.messages.feature.contacts.ContactsActivity
import org.prauga.messages.feature.contacts.ContactsActivityModule
import org.prauga.messages.feature.conversationinfo.ConversationInfoActivity
import org.prauga.messages.feature.gallery.GalleryActivity
import org.prauga.messages.feature.gallery.GalleryActivityModule
import org.prauga.messages.feature.main.MainActivity
import org.prauga.messages.feature.main.MainActivityModule
import org.prauga.messages.feature.notificationprefs.NotificationPrefsActivity
import org.prauga.messages.feature.notificationprefs.NotificationPrefsActivityModule
import org.prauga.messages.feature.plus.PlusActivity
import org.prauga.messages.feature.plus.PlusActivityModule
import org.prauga.messages.feature.qkreply.QkReplyActivity
import org.prauga.messages.feature.qkreply.QkReplyActivityModule
import org.prauga.messages.feature.scheduled.ScheduledActivity
import org.prauga.messages.feature.scheduled.ScheduledActivityModule
import org.prauga.messages.feature.settings.SettingsActivity
import org.prauga.messages.injection.scope.ActivityScope

@Module
abstract class ActivityBuilderModule {

    @ActivityScope
    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindMainActivity(): MainActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [PlusActivityModule::class])
    abstract fun bindPlusActivity(): PlusActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [])
    abstract fun bindBackupActivity(): BackupActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [ComposeActivityModule::class])
    abstract fun bindComposeActivity(): ComposeActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [ContactsActivityModule::class])
    abstract fun bindContactsActivity(): ContactsActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [])
    abstract fun bindConversationInfoActivity(): ConversationInfoActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [GalleryActivityModule::class])
    abstract fun bindGalleryActivity(): GalleryActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [NotificationPrefsActivityModule::class])
    abstract fun bindNotificationPrefsActivity(): NotificationPrefsActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [QkReplyActivityModule::class])
    abstract fun bindQkReplyActivity(): QkReplyActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [ScheduledActivityModule::class])
    abstract fun bindScheduledActivity(): ScheduledActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [])
    abstract fun bindSettingsActivity(): SettingsActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [])
    abstract fun bindBlockingActivity(): BlockingActivity

}
