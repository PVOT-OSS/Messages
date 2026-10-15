/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.injection.android

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.prauga.messages.feature.backup.BackupActivity
import org.prauga.messages.feature.blocking.BlockingActivity
import org.prauga.messages.feature.classzero.ClassZeroActivity
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
    @ContributesAndroidInjector(modules = [])
    abstract fun bindClassZeroActivity(): ClassZeroActivity

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
