/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import com.bumptech.glide.gifdecoder.GifDecoder
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.gifencoder.AnimatedGifEncoder
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.gif.GifBitmapProvider
import com.bumptech.glide.load.resource.gif.GifDrawable
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Allows converting an animated [GifDrawable] to an [OutputStream] Based on Glide's
 * ReEncodingGifResourceEncoder
 */
class GifEncoder
internal constructor(
    private val context: Context,
    private val bitmapPool: BitmapPool,
    private val factory: Factory = Factory(),
) {

    private val provider = GifBitmapProvider(bitmapPool)

    fun encodeTransformedToStream(drawable: GifDrawable, os: OutputStream): Boolean {
        val transformation = drawable.frameTransformation
        val decoder = decodeHeaders(drawable.buffer)
        val encoder = factory.buildEncoder()
        if (!encoder.start(os)) {
            return false
        }

        for (i in 0 until decoder.frameCount) {
            val currentFrame = decoder.nextFrame
            val transformedResource = getTransformedFrame(currentFrame, transformation, drawable)
            try {
                if (!encoder.addFrame(transformedResource.get())) {
                    return false
                }
                val currentFrameIndex = decoder.currentFrameIndex
                val delay = decoder.getDelay(currentFrameIndex)
                encoder.setDelay(delay)

                decoder.advance()
            } finally {
                transformedResource.recycle()
            }
        }

        return encoder.finish()
    }

    private fun decodeHeaders(data: ByteBuffer): GifDecoder {
        val parser = factory.buildParser()
        parser.setData(data)
        val header = parser.parseHeader()

        val decoder = factory.buildDecoder(provider)
        decoder.setData(header, data)
        decoder.advance()

        return decoder
    }

    private fun getTransformedFrame(
        currentFrame: Bitmap?,
        transformation: Transformation<Bitmap>,
        drawable: GifDrawable,
    ): Resource<Bitmap> {
        // TODO: what if current frame is null?
        val bitmapResource = factory.buildFrameResource(currentFrame!!, bitmapPool)
        val transformedResource =
            transformation.transform(
                context,
                bitmapResource,
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
            )
        if (bitmapResource != transformedResource) {
            bitmapResource.recycle()
        }
        return transformedResource
    }

    @VisibleForTesting
    internal class Factory {
        fun buildDecoder(bitmapProvider: GifDecoder.BitmapProvider) =
            StandardGifDecoder(bitmapProvider)

        fun buildParser() = GifHeaderParser()

        fun buildEncoder() = AnimatedGifEncoder()

        fun buildFrameResource(bitmap: Bitmap, bitmapPool: BitmapPool) =
            BitmapResource(bitmap, bitmapPool)
    }
}
