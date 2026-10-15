/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import org.prauga.messages.R
import org.prauga.messages.blocking.BlockingClient
import org.prauga.messages.interactor.MarkBlocked
import org.prauga.messages.interactor.MarkUnblocked
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.util.Preferences

// TODO: Once we have a custom dialog based on conductor, turn this into a controller
class BlockingDialog
@Inject
constructor(
    private val blockingManager: BlockingClient,
    private val context: Context,
    private val conversationRepo: ConversationRepository,
    private val prefs: Preferences,
    private val markBlocked: MarkBlocked,
    private val markUnblocked: MarkUnblocked,
) {

    fun show(activity: ComponentActivity, conversationIds: List<Long>, block: Boolean) =
        activity.lifecycleScope.launch {
            val addresses =
                conversationIds
                    .toLongArray()
                    .let { conversationRepo.getConversations(*it) }
                    .flatMap { conversation -> conversation.recipients }
                    .map { it.address }
                    .distinct()

            if (addresses.isEmpty()) {
                return@launch
            }

            if (
                blockingManager.getClientCapability() ==
                    BlockingClient.Capability.BLOCK_WITHOUT_PERMISSION
            ) {
                // If we can block/unblock in the external manager, then just fire that off and exit
                if (block) {
                    markBlocked.execute(
                        MarkBlocked.Params(conversationIds, prefs.blockingManager.get(), null)
                    )
                    blockingManager.block(addresses).subscribeOn(Schedulers.io()).subscribe()
                } else {
                    markUnblocked.execute(conversationIds)
                    blockingManager.unblock(addresses).subscribeOn(Schedulers.io()).subscribe()
                }
            } else if (block == allBlocked(addresses)) {
                // If all of the addresses are already in their correct state in the blocking
                // manager, just marked the
                // conversations blocked and exit
                when (block) {
                    true ->
                        markBlocked.execute(
                            MarkBlocked.Params(conversationIds, prefs.blockingManager.get(), null)
                        )
                    false -> markUnblocked.execute(conversationIds)
                }
            } else {
                // Otherwise, show the UI that lets the users know they need to mark the number as
                // blocked in the client
                showDialog(activity, conversationIds, addresses, block)
            }
        }

    private suspend fun allBlocked(addresses: List<String>): Boolean =
        addresses.all { address ->
            blockingManager.isBlacklisted(address).await() is BlockingClient.Action.Block
        }

    private suspend fun showDialog(
        activity: ComponentActivity,
        conversationIds: List<Long>,
        addresses: List<String>,
        block: Boolean,
    ) =
        withContext(Dispatchers.Main) {
            val res =
                when (block) {
                    true -> R.plurals.blocking_block_external
                    false -> R.plurals.blocking_unblock_external
                }

            val manager =
                context.getString(
                    when (prefs.blockingManager.get()) {
                        Preferences.BLOCKING_MANAGER_CB ->
                            R.string.blocking_manager_call_blocker_title
                        Preferences.BLOCKING_MANAGER_CC ->
                            R.string.blocking_manager_call_control_title
                        Preferences.BLOCKING_MANAGER_SIA -> R.string.blocking_manager_sia_title
                        else -> R.string.app_name
                    }
                )

            val message = context.resources.getQuantityString(res, addresses.size, manager)

            // Otherwise, show a dialog asking the user if they want to be directed to the external
            // blocking manager
            AlertDialog.Builder(activity!!, R.style.AppThemeDialog)
                .setTitle(
                    when (block) {
                        true -> R.string.blocking_block_title
                        false -> R.string.blocking_unblock_title
                    }
                )
                .setMessage(message)
                .setPositiveButton(R.string.button_continue) { _, _ ->
                    if (block) {
                        markBlocked.execute(
                            MarkBlocked.Params(conversationIds, prefs.blockingManager.get(), null)
                        )
                        blockingManager.block(addresses).subscribeOn(Schedulers.io()).subscribe()
                    } else {
                        markUnblocked.execute(conversationIds)
                        blockingManager.unblock(addresses).subscribeOn(Schedulers.io()).subscribe()
                    }
                }
                .setNegativeButton(R.string.button_cancel) { _, _ -> }
                .create()
                .show()
        }
}
