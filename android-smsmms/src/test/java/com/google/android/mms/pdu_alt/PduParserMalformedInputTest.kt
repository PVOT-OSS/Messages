/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package com.google.android.mms.pdu_alt

import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PduParserMalformedInputTest {

    private fun parse(vararg bytes: Int) =
        PduParser(ByteArray(bytes.size) { bytes[it].toByte() }, true).parse()

    @Test
    fun givenGarbageResponse_whenParse_thenReturnsNull() {
        assertNull(PduParser("\u0001\u0002not an mms".toByteArray(), true).parse())
    }

    @Test
    fun givenTruncatedFromHeader_whenParse_thenReturnsNull() {
        assertNull(parse(0x8C, 0x82, 0x89, 0x20))
    }

    @Test
    fun givenUnknownShortContentType_whenParse_thenReturnsNull() {
        assertNull(parse(0x8C, 0x82, 0x8D, 0x92, 0x84, 0xFF))
    }

    @Test
    fun givenTruncatedTransactionId_whenParse_thenReturnsNull() {
        assertNull(parse(0x8C, 0x82, 0x98, 0x41, 0x42))
    }

    @Test
    fun givenHugeDeclaredLength_whenParse_thenReturnsNull() {
        assertNull(parse(0x8C, 0x84, 0x8D, 0x92, 0x84, 0x1F, 0x8F, 0xFF, 0xFF, 0x7F, 0x01))
    }
}
