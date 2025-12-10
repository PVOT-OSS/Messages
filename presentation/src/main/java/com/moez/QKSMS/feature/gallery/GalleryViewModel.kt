/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
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

package org.prauga.messages.feature.gallery

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.moez.QKSMS.common.base.PvotViewModel
import com.moez.QKSMS.contentproviders.MmsPartProvider
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.util.extensions.makeToast
import org.prauga.messages.interactor.SaveImage
import org.prauga.messages.manager.PermissionManager
import org.prauga.messages.model.MmsPart
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository
import javax.inject.Inject
import javax.inject.Named

class GalleryViewModel @Inject constructor(
    conversationRepo: ConversationRepository,
    @Named("partId") private val partId: Long,
    private val context: Context,
    private val messageRepo: MessageRepository,
    private val navigator: Navigator,
    private val saveImage: SaveImage,
    private val permissions: PermissionManager
) : PvotViewModel<GalleryState>(GalleryState()) {

    companion object {
        const val DEFAULT_SHARE_FILENAME = "quik-media-attachment.jpg"
    }

    private var latestPart: MmsPart? = null

    init {
        viewModelScope.launch {
            val message = messageRepo.getMessageForPart(partId) ?: return@launch
            val threadId = message.threadId ?: return@launch
            val parts = messageRepo.getPartsForConversation(threadId)
            val title = conversationRepo.getConversation(threadId)?.getTitle()

            newState { copy(parts = parts) }
            newState { copy(title = title) }
        }
    }

    fun bindView(view: GalleryView) {
        super.bindView(view)

        // share options menu stream
        val optionsFlow: SharedFlow<Int> =
            view.optionsItemSelected()
                .shareIn(viewModelScope, started = SharingStarted.Eagerly, replay = 0)

        viewModelScope.launch {
            view.pageChanged()
                .collect { part ->
                    latestPart = part
                }
        }

        // When the screen is touched, toggle the visibility of the navigation UI
        viewModelScope.launch {
            view.screenTouched()
                .collect {
                    val current = state.value
                    newState { copy(navigationVisible = !current.navigationVisible) }
                }
        }

        // Save image to device
        viewModelScope.launch {
            optionsFlow
                .filter { it == R.id.save }
                .collect {
                    val part = latestPart ?: return@collect

                    if (!permissions.hasStorage()) {
                        view.requestStoragePermission()
                        return@collect
                    }
                    saveImage.execute(part.id) {
                        context.makeToast(R.string.gallery_toast_saved)
                    }
                }
        }

        // Share image externally
        viewModelScope.launch {
            optionsFlow
                .filter { it == R.id.share }
                .collect {
                    val part = latestPart ?: return@collect

                    navigator.shareFile(
                        MmsPartProvider.getUriForMmsPartId(part.id, part.getBestFilename()),
                        part.type
                    )
                }
        }

        // message part context menu item selected - forward
        viewModelScope.launch {
            optionsFlow
                .filter { it == R.id.forward }
                .collect {
                    val part = latestPart ?: return@collect
                    navigator.showCompose("", listOf(part.getUri()))
                }
        }

        // message part context menu item selected - open externally
        viewModelScope.launch {
            optionsFlow
                .filter { it == R.id.openExternally }
                .collect {
                    val part = latestPart ?: return@collect

                    navigator.viewFile(
                        MmsPartProvider.getUriForMmsPartId(part.id, part.getBestFilename()),
                        part.type
                    )
                }
        }
    }
}
