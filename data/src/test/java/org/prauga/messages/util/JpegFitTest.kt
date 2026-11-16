/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.prauga.messages.util.JpegFit.Size

class JpegFitTest {

    private class FakeEncoder {
        val calls = mutableListOf<Pair<Size, Int>>()

        fun encode(size: Size, quality: Int): ByteArray {
            calls += size to quality
            return ByteArray(size.width * size.height / 100 * quality / 10)
        }
    }

    @Test
    fun givenALandscapePhoto_whenFitToTheCarrierBox_thenItKeepsItsShape() {
        assertThat(JpegFit.fit(4160, 3120, 640, 480, 2048)).isEqualTo(Size(640, 480))
    }

    @Test
    fun givenAPortraitPhoto_whenFitToALandscapeBox_thenTheBoxTurnsWithIt() {
        assertThat(JpegFit.fit(3120, 4160, 640, 480, 2048)).isEqualTo(Size(480, 640))
    }

    @Test
    fun givenNoCarrierBox_whenFit_thenTheLongEdgeIsCapped() {
        val size = JpegFit.fit(4160, 3120, Int.MAX_VALUE, Int.MAX_VALUE, 2048)
        assertThat(size).isEqualTo(Size(2048, 1536))
    }

    @Test
    fun givenASmallImage_whenFit_thenItIsNeverEnlarged() {
        assertThat(JpegFit.fit(300, 200, 640, 480, 2048)).isEqualTo(Size(300, 200))
    }

    @Test
    fun givenRoomToSpare_whenCompressed_thenOneEncodeAtTheFirstQuality() {
        val encoder = FakeEncoder()

        val bytes = JpegFit.compress(Size(640, 480), 1_000_000, encoder::encode)

        assertThat(encoder.calls).containsExactly(Size(640, 480) to JpegFit.FIRST_QUALITY)
        assertThat(bytes).hasLength(640 * 480 / 100 * 9)
    }

    @Test
    fun givenATightBudget_whenCompressed_thenQualityDropsBeforeTheSize() {
        val encoder = FakeEncoder()
        val budget = 2048L * 1536 / 100 * 7

        val bytes = JpegFit.compress(Size(2048, 1536), budget, encoder::encode)

        assertThat(encoder.calls.map { it.first }.toSet()).containsExactly(Size(2048, 1536))
        assertThat(checkNotNull(bytes).size.toLong()).isAtMost(budget)
        assertThat(encoder.calls.last().second).isAtLeast(JpegFit.MIN_QUALITY)
    }

    @Test
    fun givenABudgetTheLowestQualityMisses_whenCompressed_thenTheSizeShrinksByHalfTheArea() {
        val encoder = FakeEncoder()
        val budget = 2048L * 1536 / 100 * 4

        val bytes = JpegFit.compress(Size(2048, 1536), budget, encoder::encode)

        assertThat(checkNotNull(bytes).size.toLong()).isAtMost(budget)
        val sizes = encoder.calls.map { it.first }.distinct()
        assertThat(sizes).containsExactly(Size(2048, 1536), Size(1448, 1086)).inOrder()
        assertThat(encoder.calls.last().second).isGreaterThan(JpegFit.MIN_QUALITY)
    }

    @Test
    fun givenAnImpossibleBudget_whenCompressed_thenNull() {
        assertThat(JpegFit.compress(Size(2048, 1536), 1, FakeEncoder()::encode)).isNull()
    }
}
