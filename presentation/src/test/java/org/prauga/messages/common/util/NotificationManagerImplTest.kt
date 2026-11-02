/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.realm.RealmList
import io.realm.RealmResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.AdditionalAnswers.delegatesTo
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.prauga.messages.manager.PermissionManager
import org.prauga.messages.manager.ShortcutManager
import org.prauga.messages.model.Conversation
import org.prauga.messages.model.Message
import org.prauga.messages.receiver.DeleteMessagesReceiver
import org.prauga.messages.repository.ContactRepository
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.util.PhoneNumberUtils
import org.prauga.messages.util.Preferences
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificationManagerImplTest {

    private val threadId = 7L
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val messageRepo = mock(MessageRepository::class.java)
    private lateinit var manager: NotificationManagerImpl

    @Before
    fun setUp() {
        val sharedPrefs = context.getSharedPreferences("notification-test", 0)
        val prefs = Preferences(context, RxSharedPreferences.create(sharedPrefs), sharedPrefs)
        val colors = mock(Colors::class.java)
        whenever(colors.theme(anyOrNull())).thenReturn(Colors.Theme(0, colors))
        val permissions = mock(PermissionManager::class.java)
        whenever(permissions.hasNotifications()).thenReturn(true)
        val conversationRepo = mock(ConversationRepository::class.java)
        whenever(conversationRepo.getConversation(threadId)).thenReturn(Conversation(id = threadId))

        manager =
            NotificationManagerImpl(
                context,
                colors,
                conversationRepo,
                prefs,
                messageRepo,
                permissions,
                PhoneNumberUtils(context),
                mock(ContactRepository::class.java),
                mock(ShortcutManager::class.java),
            )
    }

    @Suppress("UNCHECKED_CAST")
    private fun unread(newestFirst: List<Message>): RealmResults<Message> =
        mock(RealmResults::class.java, delegatesTo<Any>(ArrayList(newestFirst)))
            as RealmResults<Message>

    private fun message(date: Long): Message =
        mock(Message::class.java).also {
            whenever(it.date).thenReturn(date)
            whenever(it.address).thenReturn("+15550100")
            whenever(it.parts).thenReturn(RealmList())
            whenever(it.getSummary()).thenReturn("message $date")
            whenever(it.getText(any())).thenReturn("message $date")
        }

    private fun posted() =
        shadowOf(context.getSystemService(NotificationManager::class.java))
            .getNotification(threadId.toInt())

    @Test
    fun givenAMillionUnread_whenUpdating_thenOnlyTheNewestFitInTheNotification() {
        val retained = NotificationCompat.MessagingStyle.MAXIMUM_RETAINED_MESSAGES
        val newestFirst = (1_000_000L downTo 1_000_001L - retained).map(::message)
        val unread = unread(newestFirst)
        whenever(messageRepo.getUnreadUnseenMessages(eq(threadId), any())).thenReturn(unread)
        whenever(messageRepo.getUnreadUnseenCount(threadId)).thenReturn(1_000_000L)

        manager.update(threadId)

        verify(messageRepo).getUnreadUnseenMessages(threadId, retained.toLong())
        val notification = posted()
        assertEquals(1_000_000, notification.number)
        val style =
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)!!
        assertEquals(newestFirst.reversed().map { it.date }, style.messages.map { it.timestamp })
    }

    @Test
    fun givenUnreadMessages_whenConfirmingDelete_thenIntentCarriesADateNotEveryId() {
        val newest = unread(listOf(message(500)))
        whenever(messageRepo.getUnreadUnseenMessages(threadId, 1)).thenReturn(newest)
        whenever(messageRepo.getUnreadUnseenCount(threadId)).thenReturn(1_000_000L)

        manager.showDeleteConfirmation(threadId)

        val yes = posted().actions.first()
        val intent = shadowOf(yes.actionIntent).savedIntent
        assertFalse(intent.hasExtra("messageIds"))
        val params = DeleteMessagesReceiver.params(intent)
        assertEquals(threadId, params.threadId)
        assertEquals(500L, params.unseenUntil)
    }
}
