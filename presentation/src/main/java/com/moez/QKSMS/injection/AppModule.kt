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

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkerFactory
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import org.prauga.messages.blocking.BlockingClient
import org.prauga.messages.blocking.BlockingManager
import org.prauga.messages.common.ViewModelFactory
import org.prauga.messages.common.util.BillingManagerImpl
import org.prauga.messages.common.util.NotificationManagerImpl
import org.prauga.messages.common.util.ShortcutManagerImpl
import org.prauga.messages.feature.conversationinfo.injection.ConversationInfoComponent
import org.prauga.messages.feature.themepicker.injection.ThemePickerComponent
import org.prauga.messages.listener.ContactAddedListener
import org.prauga.messages.listener.ContactAddedListenerImpl
import org.prauga.messages.manager.ActiveConversationManager
import org.prauga.messages.manager.ActiveConversationManagerImpl
import org.prauga.messages.manager.AlarmManager
import org.prauga.messages.manager.AlarmManagerImpl
import org.prauga.messages.manager.BillingManager
import org.prauga.messages.manager.ChangelogManager
import org.prauga.messages.manager.ChangelogManagerImpl
import org.prauga.messages.manager.KeyManager
import org.prauga.messages.manager.KeyManagerImpl
import org.prauga.messages.manager.NotificationManager
import org.prauga.messages.manager.PermissionManager
import org.prauga.messages.manager.PermissionManagerImpl
import org.prauga.messages.manager.RatingManager
import org.prauga.messages.manager.ReferralManager
import org.prauga.messages.manager.ReferralManagerImpl
import org.prauga.messages.manager.ShortcutManager
import org.prauga.messages.manager.WidgetManager
import org.prauga.messages.manager.WidgetManagerImpl
import org.prauga.messages.mapper.CursorToContact
import org.prauga.messages.mapper.CursorToContactGroup
import org.prauga.messages.mapper.CursorToContactGroupImpl
import org.prauga.messages.mapper.CursorToContactGroupMember
import org.prauga.messages.mapper.CursorToContactGroupMemberImpl
import org.prauga.messages.mapper.CursorToContactImpl
import org.prauga.messages.mapper.CursorToConversation
import org.prauga.messages.mapper.CursorToConversationImpl
import org.prauga.messages.mapper.CursorToMessage
import org.prauga.messages.mapper.CursorToMessageImpl
import org.prauga.messages.mapper.CursorToPart
import org.prauga.messages.mapper.CursorToPartImpl
import org.prauga.messages.mapper.CursorToRecipient
import org.prauga.messages.mapper.CursorToRecipientImpl
import org.prauga.messages.mapper.RatingManagerImpl
import org.prauga.messages.repository.BackupRepository
import org.prauga.messages.repository.BackupRepositoryImpl
import org.prauga.messages.repository.BlockingRepository
import org.prauga.messages.repository.BlockingRepositoryImpl
import org.prauga.messages.repository.ContactRepository
import org.prauga.messages.repository.ContactRepositoryImpl
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.ConversationRepositoryImpl
import org.prauga.messages.repository.EmojiReactionRepository
import org.prauga.messages.repository.EmojiReactionRepositoryImpl
import org.prauga.messages.repository.MessageContentFilterRepository
import org.prauga.messages.repository.MessageContentFilterRepositoryImpl
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.repository.MessageRepositoryImpl
import org.prauga.messages.repository.ScheduledMessageRepository
import org.prauga.messages.repository.ScheduledMessageRepositoryImpl
import org.prauga.messages.repository.SyncRepository
import org.prauga.messages.repository.SyncRepositoryImpl
import org.prauga.messages.worker.InjectionWorkerFactory
import javax.inject.Singleton

@Module(subcomponents = [
    ConversationInfoComponent::class,
    ThemePickerComponent::class])
class AppModule(private var application: Application) {

    @Provides
    @Singleton
    fun provideContext(): Context = application

    @Provides
    fun provideContentResolver(context: Context): ContentResolver = context.contentResolver

    @Provides
    @Singleton
    fun provideSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun provideRxPreferences(preferences: SharedPreferences): RxSharedPreferences {
        return RxSharedPreferences.create(preferences)
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
    }

    @Provides
    fun provideViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory = factory

    // Listener

    @Provides
    fun provideContactAddedListener(listener: ContactAddedListenerImpl): ContactAddedListener = listener

    // Manager

    @Provides
    fun provideBillingManager(manager: BillingManagerImpl): BillingManager = manager

    @Provides
    fun provideActiveConversationManager(manager: ActiveConversationManagerImpl): ActiveConversationManager = manager

    @Provides
    fun provideAlarmManager(manager: AlarmManagerImpl): AlarmManager = manager

    @Provides
    fun blockingClient(manager: BlockingManager): BlockingClient = manager

    @Provides
    fun changelogManager(manager: ChangelogManagerImpl): ChangelogManager = manager

    @Provides
    fun provideKeyManager(manager: KeyManagerImpl): KeyManager = manager

    @Provides
    fun provideNotificationsManager(manager: NotificationManagerImpl): NotificationManager = manager

    @Provides
    fun providePermissionsManager(manager: PermissionManagerImpl): PermissionManager = manager

    @Provides
    fun provideRatingManager(manager: RatingManagerImpl): RatingManager = manager

    @Provides
    fun provideShortcutManager(manager: ShortcutManagerImpl): ShortcutManager = manager

    @Provides
    fun provideReferralManager(manager: ReferralManagerImpl): ReferralManager = manager

    @Provides
    fun provideWidgetManager(manager: WidgetManagerImpl): WidgetManager = manager

    // Mapper

    @Provides
    fun provideCursorToContact(mapper: CursorToContactImpl): CursorToContact = mapper

    @Provides
    fun provideCursorToContactGroup(mapper: CursorToContactGroupImpl): CursorToContactGroup = mapper

    @Provides
    fun provideCursorToContactGroupMember(mapper: CursorToContactGroupMemberImpl): CursorToContactGroupMember = mapper

    @Provides
    fun provideCursorToConversation(mapper: CursorToConversationImpl): CursorToConversation = mapper

    @Provides
    fun provideCursorToMessage(mapper: CursorToMessageImpl): CursorToMessage = mapper

    @Provides
    fun provideCursorToPart(mapper: CursorToPartImpl): CursorToPart = mapper

    @Provides
    fun provideCursorToRecipient(mapper: CursorToRecipientImpl): CursorToRecipient = mapper

    // Repository

    @Provides
    fun provideBackupRepository(repository: BackupRepositoryImpl): BackupRepository = repository

    @Provides
    fun provideBlockingRepository(repository: BlockingRepositoryImpl): BlockingRepository = repository

    @Provides
    fun provideMessageContentFilterRepository(repository: MessageContentFilterRepositoryImpl): MessageContentFilterRepository = repository

    @Provides
    fun provideContactRepository(repository: ContactRepositoryImpl): ContactRepository = repository

    @Provides
    fun provideConversationRepository(repository: ConversationRepositoryImpl): ConversationRepository = repository

    @Provides
    fun provideMessageRepository(repository: MessageRepositoryImpl): MessageRepository = repository

    @Provides
    fun provideScheduledMessagesRepository(repository: ScheduledMessageRepositoryImpl): ScheduledMessageRepository = repository

    @Provides
    fun provideSyncRepository(repository: SyncRepositoryImpl): SyncRepository = repository

    @Provides
    fun provideEmojiReactionRepository(repository: EmojiReactionRepositoryImpl): EmojiReactionRepository = repository

    // worker factory
    @Provides
    fun provideWorkerFactory(workerFactory: InjectionWorkerFactory): WorkerFactory = workerFactory
}