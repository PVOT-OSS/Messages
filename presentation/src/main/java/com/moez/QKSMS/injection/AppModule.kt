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
package dev.danascape.messages.injection

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
import dev.danascape.messages.blocking.BlockingClient
import dev.danascape.messages.blocking.BlockingManager
import dev.danascape.messages.common.ViewModelFactory
import dev.danascape.messages.common.util.BillingManagerImpl
import dev.danascape.messages.common.util.NotificationManagerImpl
import dev.danascape.messages.common.util.ShortcutManagerImpl
import dev.danascape.messages.feature.conversationinfo.injection.ConversationInfoComponent
import dev.danascape.messages.feature.themepicker.injection.ThemePickerComponent
import dev.danascape.messages.listener.ContactAddedListener
import dev.danascape.messages.listener.ContactAddedListenerImpl
import dev.danascape.messages.manager.ActiveConversationManager
import dev.danascape.messages.manager.ActiveConversationManagerImpl
import dev.danascape.messages.manager.AlarmManager
import dev.danascape.messages.manager.AlarmManagerImpl
import dev.danascape.messages.manager.BillingManager
import dev.danascape.messages.manager.ChangelogManager
import dev.danascape.messages.manager.ChangelogManagerImpl
import dev.danascape.messages.manager.KeyManager
import dev.danascape.messages.manager.KeyManagerImpl
import dev.danascape.messages.manager.NotificationManager
import dev.danascape.messages.manager.PermissionManager
import dev.danascape.messages.manager.PermissionManagerImpl
import dev.danascape.messages.manager.RatingManager
import dev.danascape.messages.manager.ReferralManager
import dev.danascape.messages.manager.ReferralManagerImpl
import dev.danascape.messages.manager.ShortcutManager
import dev.danascape.messages.manager.WidgetManager
import dev.danascape.messages.manager.WidgetManagerImpl
import dev.danascape.messages.mapper.CursorToContact
import dev.danascape.messages.mapper.CursorToContactGroup
import dev.danascape.messages.mapper.CursorToContactGroupImpl
import dev.danascape.messages.mapper.CursorToContactGroupMember
import dev.danascape.messages.mapper.CursorToContactGroupMemberImpl
import dev.danascape.messages.mapper.CursorToContactImpl
import dev.danascape.messages.mapper.CursorToConversation
import dev.danascape.messages.mapper.CursorToConversationImpl
import dev.danascape.messages.mapper.CursorToMessage
import dev.danascape.messages.mapper.CursorToMessageImpl
import dev.danascape.messages.mapper.CursorToPart
import dev.danascape.messages.mapper.CursorToPartImpl
import dev.danascape.messages.mapper.CursorToRecipient
import dev.danascape.messages.mapper.CursorToRecipientImpl
import dev.danascape.messages.mapper.RatingManagerImpl
import dev.danascape.messages.repository.BackupRepository
import dev.danascape.messages.repository.BackupRepositoryImpl
import dev.danascape.messages.repository.BlockingRepository
import dev.danascape.messages.repository.BlockingRepositoryImpl
import dev.danascape.messages.repository.ContactRepository
import dev.danascape.messages.repository.ContactRepositoryImpl
import dev.danascape.messages.repository.ConversationRepository
import dev.danascape.messages.repository.ConversationRepositoryImpl
import dev.danascape.messages.repository.EmojiReactionRepository
import dev.danascape.messages.repository.EmojiReactionRepositoryImpl
import dev.danascape.messages.repository.MessageContentFilterRepository
import dev.danascape.messages.repository.MessageContentFilterRepositoryImpl
import dev.danascape.messages.repository.MessageRepository
import dev.danascape.messages.repository.MessageRepositoryImpl
import dev.danascape.messages.repository.ScheduledMessageRepository
import dev.danascape.messages.repository.ScheduledMessageRepositoryImpl
import dev.danascape.messages.repository.SyncRepository
import dev.danascape.messages.repository.SyncRepositoryImpl
import dev.danascape.messages.worker.InjectionWorkerFactory
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