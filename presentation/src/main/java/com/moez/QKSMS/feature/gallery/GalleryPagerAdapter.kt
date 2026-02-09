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
package org.prauga.messages.feature.gallery

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.mms.ContentType
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.prauga.messages.R
import org.prauga.messages.common.base.QkRealmAdapter
import org.prauga.messages.common.base.QkViewHolder
import org.prauga.messages.databinding.GalleryImagePageBinding
import org.prauga.messages.databinding.GalleryVideoPageBinding
import org.prauga.messages.extensions.isImage
import org.prauga.messages.extensions.isVideo
import org.prauga.messages.model.MmsPart
import org.prauga.messages.util.GlideApp
import java.util.Collections
import java.util.WeakHashMap
import javax.inject.Inject

class GalleryPagerAdapter @Inject constructor(private val context: Context) :
    QkRealmAdapter<MmsPart, QkViewHolder>() {

    companion object {
        private const val VIEW_TYPE_INVALID = 0
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_VIDEO = 2
    }

    val clicks: Subject<View> = PublishSubject.create()

    private val contentResolver = context.contentResolver
    private val exoPlayers = Collections.newSetFromMap(WeakHashMap<ExoPlayer?, Boolean>())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return QkViewHolder(
            when (viewType) {
            VIEW_TYPE_IMAGE -> {
                val binding = GalleryImagePageBinding.inflate(inflater, parent, false)
                // Use a tiny offset for midScale since setScaleLevels requires min < mid < max
                binding.image.attacher.setScaleLevels(1f, 1.01f, 3f)
                binding.root.apply { setOnClickListener(clicks::onNext) }
            }

            VIEW_TYPE_VIDEO -> {
                val binding = GalleryVideoPageBinding.inflate(inflater, parent, false)
                binding.root.apply { setOnClickListener(clicks::onNext) }
            }

            else -> inflater.inflate(R.layout.gallery_invalid_page, parent, false).apply {
                setOnClickListener(clicks::onNext)
            }
        })
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val part = getItem(position) ?: return
        when (getItemViewType(position)) {
            VIEW_TYPE_IMAGE -> {
                val binding = GalleryImagePageBinding.bind(holder.containerView)
                // We need to explicitly request a gif from glide for animations to work
                when (part.getUri().let(contentResolver::getType)) {
                    ContentType.IMAGE_GIF -> GlideApp.with(context)
                        .asGif()
                        .load(part.getUri())
                        .into(binding.image)

                    else -> GlideApp.with(context)
                        .asBitmap()
                        .load(part.getUri())
                        .into(binding.image)
                }
            }

            VIEW_TYPE_VIDEO -> {
                val binding = GalleryVideoPageBinding.bind(holder.containerView)
                val exoPlayer = ExoPlayer.Builder(context).build()
                binding.video.player = exoPlayer
                exoPlayers.add(exoPlayer)

                val mediaItem = MediaItem.fromUri(part.getUri())
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val part = getItem(position)
        return when {
            part?.isImage() == true -> VIEW_TYPE_IMAGE
            part?.isVideo() == true -> VIEW_TYPE_VIDEO
            else -> VIEW_TYPE_INVALID
        }
    }

    fun destroy() {
        exoPlayers.forEach { exoPlayer -> exoPlayer?.release() }
    }

}
