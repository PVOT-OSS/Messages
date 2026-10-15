/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.klinker.android.send_message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmilBuilderTest {

    private val image = SmilBuilder.Part("image/jpeg", "image000000.jpg")
    private val text = SmilBuilder.Part("text/plain", "text000001.txt")

    @Test
    fun givenImageAndText_whenBuilt_thenImageAboveTextEachForFiveSeconds() {
        val smil = SmilBuilder.build(listOf(image, text))

        assertTrue(smil.contains("<region id=\"Image\" top=\"0%\" left=\"0%\" height=\"80%\""))
        assertTrue(smil.contains("<region id=\"Text\" top=\"80%\" left=\"0%\" height=\"20%\""))
        assertTrue(
            smil.contains(
                "<body><par dur=\"5000ms\"><img src=\"image000000.jpg\" region=\"Image\"/></par>" +
                    "<par dur=\"5000ms\"><text src=\"text000001.txt\" region=\"Text\"/></par></body>"
            )
        )
    }

    @Test
    fun givenOnlyAnImage_whenBuilt_thenItTakesTheWholeScreen() {
        val smil = SmilBuilder.build(listOf(image))

        assertTrue(smil.contains("<region id=\"Image\" top=\"0%\" left=\"0%\" height=\"100%\""))
        assertTrue(!smil.contains("id=\"Text\""))
    }

    @Test
    fun givenOnlyText_whenBuilt_thenItTakesTheWholeScreen() {
        assertTrue(SmilBuilder.build(listOf(text)).contains("<region id=\"Text\" top=\"0%\""))
    }

    @Test
    fun givenOtherMedia_whenBuilt_thenEachHasItsElement() {
        val smil =
            SmilBuilder.build(
                listOf(
                    SmilBuilder.Part("video/mp4", "v.mp4"),
                    SmilBuilder.Part("audio/amr", "a.amr"),
                    SmilBuilder.Part("text/x-vCard", "c.vcf"),
                )
            )

        assertTrue(smil.contains("<video src=\"v.mp4\" region=\"Image\"/>"))
        assertTrue(smil.contains("<audio src=\"a.amr\"/>"))
        assertTrue(smil.contains("<ref src=\"c.vcf\"/>"))
    }

    @Test
    fun givenANameWithQuotes_whenBuilt_thenItIsEscaped() {
        assertEquals(
            true,
            SmilBuilder.build(listOf(SmilBuilder.Part("image/png", "a\"b&.png")))
                .contains("src=\"a&quot;b&amp;.png\""),
        )
    }
}
