/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.qkreply

import android.telephony.SmsMessage
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import io.realm.RealmResults
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkViewModel
import org.prauga.messages.compat.SubscriptionManagerCompat
import org.prauga.messages.extensions.asObservable
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.interactor.DeleteMessages
import org.prauga.messages.interactor.MarkRead
import org.prauga.messages.interactor.SendMessage
import org.prauga.messages.model.Message
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository
import org.prauga.messages.util.ActiveSubscriptionObservable

class QkReplyViewModel
@Inject
constructor(
    @Named("threadId") private val threadId: Long,
    private val conversationRepo: ConversationRepository,
    private val deleteMessages: DeleteMessages,
    private val markRead: MarkRead,
    private val messageRepo: MessageRepository,
    private val navigator: Navigator,
    private val sendMessage: SendMessage,
    private val subscriptionManager: SubscriptionManagerCompat,
) : QkViewModel<QkReplyView, QkReplyState>(QkReplyState(threadId = threadId)) {

    private val conversation by lazy {
        conversationRepo
            .getConversationAsync(threadId)
            .asObservable()
            .filter { it.isLoaded }
            .filter { it.isValid }
            .distinctUntilChanged()
    }

    private val messages: Subject<RealmResults<Message>> =
        BehaviorSubject.createDefault(messageRepo.getUnreadMessages(threadId))

    init {
        disposables += markRead
        disposables += sendMessage

        // When the set of messages changes, update the state
        // If we're ever showing an empty set of messages, then it's time to shut down to activity
        disposables +=
            Observables.combineLatest(messages, conversation) { messages, conversation ->
                    newState { copy(data = Pair(conversation, messages)) }
                    messages
                }
                .switchMap { messages -> messages.asObservable() }
                .filter { it.isLoaded }
                .filter { it.isValid }
                .filter { it.isEmpty() }
                .subscribe { newState { copy(hasError = true) } }

        disposables +=
            conversation
                .map { conversation -> conversation.getTitle() }
                .distinctUntilChanged()
                .subscribe { title -> newState { copy(title = title) } }

        val latestSubId =
            messages.map { messages -> messages.lastOrNull()?.subId ?: -1 }.distinctUntilChanged()

        val subscriptions = ActiveSubscriptionObservable(subscriptionManager)
        disposables +=
            Observables.combineLatest(latestSubId, subscriptions) { subId, subs ->
                    val sub =
                        if (subs.size > 1)
                            SubscriptionManagerCompat.preferredSubscription(
                                subs,
                                subId,
                                subscriptionManager.defaultSmsSubscriptionId,
                            )
                        else null
                    newState { copy(subscription = sub) }
                }
                .subscribe()
    }

    override fun bindView(view: QkReplyView) {
        super.bindView(view)

        conversation
            .take(1) // only update saved draft to ui once
            .map { conversation -> conversation.draft }
            .distinctUntilChanged()
            .autoDispose(view.scope())
            .subscribe { draft -> view.setDraft(draft) }

        // Mark read
        view.menuItemIntent
            .filter { id -> id == R.id.read }
            .autoDispose(view.scope())
            .subscribe { markRead.execute(listOf(threadId)) { newState { copy(hasError = true) } } }

        // Call
        view.menuItemIntent
            .filter { id -> id == R.id.call }
            .withLatestFrom(state, conversation)
            .mapNotNull { (_, state, conversation) ->
                state.data
                    ?.second
                    ?.lastOrNull { !it.isMe() }
                    ?.address // most recent non-me msg address
                ?: conversation.recipients.firstOrNull()?.address // first recipient in convo
            }
            .doOnNext { navigator.makePhoneCall(it) }
            .autoDispose(view.scope())
            .subscribe { newState { copy(hasError = true) } }

        // Show all messages
        view.menuItemIntent
            .filter { id -> id == R.id.expand }
            .map { messageRepo.getMessages(threadId) }
            .doOnNext(messages::onNext)
            .autoDispose(view.scope())
            .subscribe { newState { copy(expanded = true) } }

        // Show unread messages only
        view.menuItemIntent
            .filter { id -> id == R.id.collapse }
            .map { messageRepo.getUnreadMessages(threadId) }
            .doOnNext(messages::onNext)
            .autoDispose(view.scope())
            .subscribe { newState { copy(expanded = false) } }

        // Delete new messages
        view.menuItemIntent
            .filter { id -> id == R.id.delete }
            .observeOn(Schedulers.io())
            .map { messageRepo.getUnreadMessages(threadId).map { it.id } }
            .map { messages -> DeleteMessages.Params(messages, threadId) }
            .autoDispose(view.scope())
            .subscribe { deleteMessages.execute(it) { newState { copy(hasError = true) } } }

        // View conversation
        view.menuItemIntent
            .filter { id -> id == R.id.view }
            .doOnNext { navigator.showConversation(threadId) }
            .autoDispose(view.scope())
            .subscribe { newState { copy(hasError = true) } }

        // Enable the send button when there is text input into the new message body or there's
        // an attachment, disable otherwise
        view.textChangedIntent
            .map { text -> text.isNotBlank() }
            .autoDispose(view.scope())
            .subscribe { canSend -> newState { copy(canSend = canSend) } }

        // Show the remaining character counter when necessary
        view.textChangedIntent
            .observeOn(Schedulers.computation())
            .map { draft -> SmsMessage.calculateLength(draft, false) }
            .map { array ->
                val messages = array[0]
                val remaining = array[2]

                when {
                    messages <= 1 && remaining > 10 -> ""
                    messages <= 1 && remaining <= 10 -> "$remaining"
                    else -> "$remaining / $messages"
                }
            }
            .distinctUntilChanged()
            .autoDispose(view.scope())
            .subscribe { remaining -> newState { copy(remaining = remaining) } }

        // Update the draft whenever the text is changed
        view.textChangedIntent
            .debounce(100, TimeUnit.MILLISECONDS)
            .map { draft -> draft.toString() }
            .observeOn(Schedulers.io())
            .autoDispose(view.scope())
            .subscribe { draft -> conversationRepo.saveDraft(threadId, draft) }

        // Toggle to the next sim slot
        view.changeSimIntent
            .withLatestFrom(state) { _, state ->
                val subs = subscriptionManager.activeSubscriptionInfoList
                val subIndex =
                    subs.indexOfFirst { it.subscriptionId == state.subscription?.subscriptionId }
                val subscription =
                    when {
                        subIndex == -1 -> null
                        subIndex < subs.size - 1 -> subs[subIndex + 1]
                        else -> subs[0]
                    }
                newState { copy(subscription = subscription) }
            }
            .autoDispose(view.scope())
            .subscribe()

        // Send a message when the send button is clicked, and disable editing mode if it's enabled
        view.sendIntent
            .withLatestFrom(view.textChangedIntent) { _, body -> body }
            .map { body -> body.toString() }
            .withLatestFrom(state, conversation) { body, state, conversation ->
                val subId = state.subscription?.subscriptionId ?: -1
                val addresses = conversation.recipients.map { it.address }
                sendMessage.execute(SendMessage.Params(subId, threadId, addresses, body))
                view.setDraft("")
            }
            .doOnNext { markRead.execute(listOf(threadId)) { newState { copy(hasError = true) } } }
            .autoDispose(view.scope())
            .subscribe()
    }
}
