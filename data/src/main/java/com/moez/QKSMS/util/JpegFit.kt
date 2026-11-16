/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object JpegFit {

    const val FIRST_QUALITY = 90
    const val MIN_QUALITY = 69
    const val MAX_QUALITY = 100
    const val START_LONG_EDGE = 2048
    private const val GOOD_ENOUGH = 0.95
    private const val MIN_SIDE = 32
    private val SHRINK = sqrt(0.5)

    data class Size(val width: Int, val height: Int)

    /** The box turns with the image: a portrait photo gets the box's long side as height */
    fun fit(width: Int, height: Int, boxWidth: Int, boxHeight: Int, longEdge: Int): Size {
        val longLimit = min(max(boxWidth, boxHeight), longEdge)
        val shortLimit = min(min(boxWidth, boxHeight), longEdge)
        val long = max(width, height)
        val short = min(width, height)
        val scale = minOf(1.0, longLimit.toDouble() / long, shortLimit.toDouble() / short)
        return Size(max(1, (width * scale).roundToInt()), max(1, (height * scale).roundToInt()))
    }

    /** Lowers quality before size; null when even [MIN_SIDE] pixels don't fit [maxBytes] */
    fun compress(size: Size, maxBytes: Long, encode: (Size, Int) -> ByteArray): ByteArray? {
        val first = encode(size, FIRST_QUALITY)
        if (first.size <= maxBytes) return first

        var current = size
        var best = encode(current, MIN_QUALITY)
        while (best.size > maxBytes) {
            current = Size((current.width * SHRINK).toInt(), (current.height * SHRINK).toInt())
            if (current.width < MIN_SIDE || current.height < MIN_SIDE) return null
            best = encode(current, MIN_QUALITY)
        }

        var fits = MIN_QUALITY
        var tooBig = if (current == size) FIRST_QUALITY else MAX_QUALITY + 1
        while (tooBig - fits > 1 && best.size < maxBytes * GOOD_ENOUGH) {
            val quality = (fits + tooBig) / 2
            val bytes = encode(current, quality)
            if (bytes.size <= maxBytes) {
                fits = quality
                best = bytes
            } else {
                tooBig = quality
            }
        }
        return best
    }
}
