/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import java.io.ByteArrayOutputStream

object ImageUtils {

    fun getScaledGif(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int = 90,
    ): ByteArray {
        val gif =
            GlideApp.with(context)
                .asGif()
                .load(uri)
                .centerInside()
                .encodeQuality(quality)
                .submit(maxWidth, maxHeight)
                .get()

        val outputStream = ByteArrayOutputStream()
        GifEncoder(context, GlideApp.get(context).bitmapPool)
            .encodeTransformedToStream(gif, outputStream)
        return outputStream.toByteArray()
    }

    fun compressImage(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        maxHeight: Int,
        maxBytes: Long,
    ): ByteArray? {
        val longEdge = if (maxBytes >= Int.MAX_VALUE) Int.MAX_VALUE else JpegFit.START_LONG_EDGE
        val decodeEdge = minOf(longEdge, maxOf(maxWidth, maxHeight))
        val request =
            GlideApp.with(context)
                .asBitmap()
                .load(uri)
                .apply(
                    RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                )
                .centerInside()
                .submit(
                    decodeEdge.takeIf { it != Int.MAX_VALUE } ?: Target.SIZE_ORIGINAL,
                    decodeEdge.takeIf { it != Int.MAX_VALUE } ?: Target.SIZE_ORIGINAL,
                )
        try {
            val decoded = request.get()
            val size = JpegFit.fit(decoded.width, decoded.height, maxWidth, maxHeight, longEdge)
            var scaled = decoded
            return JpegFit.compress(size, maxBytes) { target, quality ->
                    if (scaled.width != target.width || scaled.height != target.height) {
                        if (scaled !== decoded) scaled.recycle()
                        scaled =
                            Bitmap.createScaledBitmap(decoded, target.width, target.height, true)
                    }
                    ByteArrayOutputStream()
                        .also { scaled.compress(Bitmap.CompressFormat.JPEG, quality, it) }
                        .toByteArray()
                }
                .also { if (scaled !== decoded) scaled.recycle() }
        } finally {
            GlideApp.with(context).clear(request)
        }
    }
}
