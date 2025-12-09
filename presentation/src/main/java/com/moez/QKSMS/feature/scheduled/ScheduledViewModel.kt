package org.prauga.messages.feature.scheduled

import android.content.Context
import com.uber.autodispose2.androidx.lifecycle.scope
import com.uber.autodispose2.autoDispose
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkViewModel
import org.prauga.messages.common.util.ClipboardUtils
import org.prauga.messages.interactor.DeleteScheduledMessages
import org.prauga.messages.interactor.SendScheduledMessage
import org.prauga.messages.manager.BillingManager
import org.prauga.messages.repository.ScheduledMessageRepository
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Named

class ScheduledViewModel @Inject constructor(
    @Named("conversationId") private val conversationId: Long?,
    billingManager: BillingManager,
    private val context: Context,
    private val navigator: Navigator,
    private val scheduledMessageRepo: ScheduledMessageRepository,
    private val sendScheduledMessageInteractor: SendScheduledMessage,
    private val deleteScheduledMessagesInteractor: DeleteScheduledMessages,
) : QkViewModel<ScheduledView, ScheduledState>(ScheduledState(
    scheduledMessages = scheduledMessageRepo.getScheduledMessages(),
    conversationId = conversationId
)) {

    init {
        loadMessages(conversationId)
        disposables += billingManager.upgradeStatus
            .subscribe { upgraded -> newState { copy(upgraded = upgraded) } }
    }

    override fun bindView(view: ScheduledView) {
        super.bindView(view)

        // update the state when the message selected count changes
        view.messagesSelectedIntent
            .map { selection -> selection.size }
            .autoDispose(view.scope())
            .subscribe { newState { copy(selectedMessages = it) } }

        // toggle select all / select none
        view.optionsItemIntent
            .filter { it == R.id.select_all }
            .autoDispose(view.scope())
            .subscribe { view.toggleSelectAll() }

        // show the delete message dialog if one or more messages selected
        view.optionsItemIntent
            .filter { it == R.id.delete }
            .withLatestFrom(view.messagesSelectedIntent) { _, selectedMessages ->
                selectedMessages }
            .autoDispose(view.scope())
            .subscribe {
                val ids = it.mapNotNull(scheduledMessageRepo::getScheduledMessage)
                    .map { it.id }
                view.showDeleteDialog(ids)
            }


        // copy the selected message text to the clipboard
        view.optionsItemIntent
            .filter { it == R.id.copy }
            .withLatestFrom(view.messagesSelectedIntent) { _, selectedMessages ->
                selectedMessages }
            .autoDispose(view.scope())
            .subscribe {
                val messages = it
                    .mapNotNull(scheduledMessageRepo::getScheduledMessage)
                    .sortedBy { it.date }   // same order as messages on screen
                val text = when (messages.size) {
                    1 -> messages.first().body
                    else -> messages.fold(StringBuilder()) { acc, message ->
                        if (acc.isNotEmpty() && message.body.isNotEmpty())
                            acc.append("\n\n")
                        acc.append(message.body)
                    }
                }

                ClipboardUtils.copy(context, text.toString())
            }

        // send the messages now menu item selected
        view.optionsItemIntent
            .filter { it == R.id.send_now }
            .withLatestFrom(view.messagesSelectedIntent) { _, selectedMessages ->
                selectedMessages }
            .autoDispose(view.scope())
            .subscribe { view.showSendNowDialog(it) }

        // edit message menu item selected
        view.optionsItemIntent
            .filter { it == R.id.edit_message }
            .withLatestFrom(view.messagesSelectedIntent) { _, selectedMessage ->
                selectedMessage.first() }
            .autoDispose(view.scope())
            .subscribe { view.showEditMessageDialog(it) }

        // delete message(s) (fired after the confirmation dialog has been shown)
        view.deleteScheduledMessages
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .autoDispose(view.scope())
            .subscribe {
                deleteScheduledMessagesInteractor.execute(it)
                view.clearSelection()
            }

        // send message(s) now (fired after the confirmation dialog has been shown)
        view.sendScheduledMessages
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .autoDispose(view.scope())
            .subscribe {
                it.forEach { sendScheduledMessageInteractor.execute(it) }
                view.clearSelection()
            }


        // edit message (fired after the confirmation dialog has been shown)
        view.editScheduledMessage
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .autoDispose(view.scope())
            .subscribe {
                scheduledMessageRepo.getScheduledMessage(it)?.let {
                    navigator.showCompose(it)
                    scheduledMessageRepo.deleteScheduledMessage(it.id)
                }
                view.clearSelection()
            }

        // navigate back or unselect
        view.optionsItemIntent
            .filter { it == android.R.id.home }
            .map { }
            .mergeWith(view.backPressedIntent)
            .withLatestFrom(state) { _, state -> state }
            .autoDispose(view.scope())
            .subscribe {
                when {
                    (it.selectedMessages > 0) -> view.clearSelection()
                    else -> view.finishActivity()
                }
            }

        view.composeIntent
            .autoDispose(view.scope())
            .subscribe {
                navigator.showCompose(mode = "scheduling")
                view.clearSelection()
            }

        view.upgradeIntent
            .autoDispose(view.scope())
            .subscribe { navigator.showQksmsPlusActivity("schedule_fab") }
    }

    private fun loadMessages(conversationId: Long?) {
        val results = if (conversationId != null)
            scheduledMessageRepo.getScheduledMessagesForConversation(conversationId)
        else
            scheduledMessageRepo.getScheduledMessages()
        newState { copy(scheduledMessages = results) }
    }
}
