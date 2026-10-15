/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: 2025 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.migration

import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.prauga.messages.blocking.QksmsBlockingClient
import org.prauga.messages.common.util.extensions.versionCode
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.util.Preferences

class QkMigration
@Inject
constructor(
    private val context: Context,
    private val conversationRepo: ConversationRepository,
    private val prefs: Preferences,
    private val qksmsBlockingClient: QksmsBlockingClient,
) {

    fun performMigration() =
        runBlocking(Dispatchers.IO) {
            val oldVersion = prefs.version.get()

            if (oldVersion < 2199) {
                upgradeTo370()
            }

            prefs.version.set(context.versionCode)
        }

    private fun upgradeTo370() {
        // Migrate changelog version
        prefs.changelogVersion.set(prefs.version.get())

        // Migrate from old SIA preference to blocking manager preference
        if (prefs.sia.get()) {
            prefs.blockingManager.set(Preferences.BLOCKING_MANAGER_SIA)
            prefs.sia.delete()
        }

        // Migrate blocked conversations into QK blocking client
        val addresses =
            conversationRepo
                .getBlockedConversations()
                .flatMap { conversation -> conversation.recipients }
                .map { recipient -> recipient.address }
                .distinct()

        qksmsBlockingClient.block(addresses).blockingAwait()
    }
}
