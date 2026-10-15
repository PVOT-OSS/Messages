/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util

import android.content.ComponentName
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.service.chooser.ChooserTarget
import android.service.chooser.ChooserTargetService
import androidx.core.os.bundleOf
import javax.inject.Inject
import org.prauga.messages.R
import org.prauga.messages.feature.compose.ComposeActivity
import org.prauga.messages.injection.appComponent
import org.prauga.messages.model.Conversation
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.util.GlideApp
import org.prauga.messages.util.tryOrNull

class QkChooserTargetService : ChooserTargetService() {

    @Inject lateinit var conversationRepo: ConversationRepository

    override fun onCreate() {
        appComponent.inject(this)
        super.onCreate()
    }

    override fun onGetChooserTargets(
        targetActivityName: ComponentName?,
        matchedFilter: IntentFilter?,
    ): List<ChooserTarget> {
        return conversationRepo
            .getTopConversations()
            .take(3)
            .map(this::createShortcutForConversation)
    }

    private fun createShortcutForConversation(conversation: Conversation): ChooserTarget {
        val icon =
            when (conversation.recipients.size) {
                1 -> {
                    val photoUri = conversation.recipients.first()?.contact?.photoUri
                    val request =
                        GlideApp.with(this).asBitmap().circleCrop().load(photoUri).submit()
                    val bitmap = tryOrNull(false) { request.get() }

                    if (bitmap != null) Icon.createWithBitmap(bitmap)
                    else Icon.createWithResource(this, R.mipmap.ic_shortcut_person)
                }

                else -> Icon.createWithResource(this, R.mipmap.ic_shortcut_people)
            }

        val componentName = ComponentName(this, ComposeActivity::class.java)

        return ChooserTarget(
            conversation.getTitle(),
            icon,
            1f,
            componentName,
            bundleOf("threadId" to conversation.id),
        )
    }
}
