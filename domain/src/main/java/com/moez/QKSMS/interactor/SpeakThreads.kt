/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.interactor

import com.moez.QKSMS.manager.SpeakManager
import io.reactivex.Flowable
import io.reactivex.Single
import javax.inject.Inject
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository

class SpeakThreads
@Inject
constructor(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
) : Interactor<List<Long>>() {

    companion object {
        private var noMessagesStr = "No messages" // default value

        fun setNoMessagesString(newNoMessagesString: String) {
            noMessagesStr = newNoMessagesString
        }
    }

    override fun buildObservable(threadIds: List<Long>): Flowable<*> {
        val speakManager = SpeakManager()

        if (threadIds.isEmpty())
            return Single.just(0)
                .doOnSubscribe { speakManager.startSpeakSession(noMessagesStr) }
                .map { speakManager.speak(noMessagesStr) }
                .doOnTerminate { speakManager.endSpeakSession() }
                .toFlowable()

        return Flowable.fromIterable(threadIds)
            .doOnSubscribe {
                speakManager.startSpeakSession("threads:" + threadIds.sorted().joinToString())
            }
            .mapNotNull { threadId ->
                conversationRepo.getConversationAndLastSenderContactName(threadId)
            }
            .map { conversationAndSender ->
                if (speakManager.speakConversationLastSms(conversationAndSender))
                    messageRepo.markSeen(conversationAndSender.first!!.id)
            }
            .doOnTerminate { speakManager.endSpeakSession() }
    }
}
