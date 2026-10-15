/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.gallery

import android.content.Context
import com.moez.QKSMS.contentproviders.MmsPartProvider
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import io.reactivex.Flowable
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject
import javax.inject.Named
import org.prauga.messages.R
import org.prauga.messages.common.Navigator
import org.prauga.messages.common.base.QkViewModel
import org.prauga.messages.common.util.FileNamingConfig
import org.prauga.messages.common.util.extensions.makeToast
import org.prauga.messages.extensions.mapNotNull
import org.prauga.messages.interactor.SaveImage
import org.prauga.messages.manager.PermissionManager
import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository

class GalleryViewModel
@Inject
constructor(
    conversationRepo: ConversationRepository,
    @Named("partId") private val partId: Long,
    private val context: Context,
    private val messageRepo: MessageRepository,
    private val navigator: Navigator,
    private val saveImage: SaveImage,
    private val permissions: PermissionManager,
) : QkViewModel<GalleryView, GalleryState>(GalleryState()) {
    companion object {
        const val DEFAULT_SHARE_FILENAME = FileNamingConfig.DEFAULT_GALLERY_SHARE_FILENAME
    }

    init {
        disposables +=
            Flowable.just(partId)
                .mapNotNull(messageRepo::getMessageForPart)
                .mapNotNull { message -> message.threadId }
                .doOnNext { threadId ->
                    newState { copy(parts = messageRepo.getPartsForConversation(threadId)) }
                }
                .doOnNext { threadId ->
                    newState {
                        copy(title = conversationRepo.getConversation(threadId)?.getTitle())
                    }
                }
                .subscribe()
    }

    override fun bindView(view: GalleryView) {
        super.bindView(view)

        // When the screen is touched, toggle the visibility of the navigation UI
        view
            .screenTouched()
            .withLatestFrom(state) { _, state -> state.navigationVisible }
            .map { navigationVisible -> !navigationVisible }
            .autoDispose(view.scope())
            .subscribe { navigationVisible ->
                newState { copy(navigationVisible = navigationVisible) }
            }

        // Save image to device
        view
            .optionsItemSelected()
            .filter { it == R.id.save }
            .filter { permissions.hasStorage().also { if (!it) view.requestStoragePermission() } }
            .withLatestFrom(view.pageChanged()) { _, part -> part.id }
            .autoDispose(view.scope())
            .subscribe { partId ->
                saveImage.execute(partId) { context.makeToast(R.string.gallery_toast_saved) }
            }

        // Share image externally
        view
            .optionsItemSelected()
            .filter { it == R.id.share }
            .withLatestFrom(view.pageChanged()) { _, part -> part }
            .autoDispose(view.scope())
            .subscribe {
                navigator.shareFile(
                    MmsPartProvider.getUriForMmsPartId(it.id, it.getBestFilename()),
                    it.type,
                )
            }

        // message part context menu item selected - forward
        view
            .optionsItemSelected()
            .filter { it == R.id.forward }
            .withLatestFrom(view.pageChanged()) { _, part -> part }
            .autoDispose(view.scope())
            .subscribe { navigator.showCompose("", listOf(it.getUri())) }

        // message part context menu item selected - open externally
        view
            .optionsItemSelected()
            .filter { it == R.id.openExternally }
            .withLatestFrom(view.pageChanged()) { _, part -> part }
            .autoDispose(view.scope())
            .subscribe {
                navigator.viewFile(
                    MmsPartProvider.getUriForMmsPartId(it.id, it.getBestFilename()),
                    it.type,
                )
            }
    }
}
