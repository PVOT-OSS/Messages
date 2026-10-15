/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.klinker.android.send_message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleCharactersTest {

    private val simple = SimpleCharacters(mapOf('á' to 'a', 'ç' to 'c', '“' to '"', '”' to '"'))

    @Test
    fun givenPlainText_whenConverted_thenUnchanged() {
        assertEquals("Hello, world!\nOK", simple.convert("Hello, world!\nOK"))
    }

    @Test
    fun givenGsmCharacters_whenConverted_thenKept() {
        assertEquals("café à £5 €", simple.convert("café à £5 €"))
    }

    @Test
    fun givenAccentsInTheTable_whenConverted_thenReplaced() {
        assertEquals("Ola \"amigo\" aca", simple.convert("Olá “amigo” açá"))
    }

    @Test
    fun givenLatinWithoutAReplacement_whenConverted_thenBecomesASpace() {
        assertEquals("a b", simple.convert("aŧb"))
        assertEquals("a b", simple.convert("a`b"))
    }

    @Test
    fun givenOtherScriptsAndEmoji_whenConverted_thenLeftAlone() {
        assertEquals("Привет 你好 👋", simple.convert("Привет 你好 👋"))
    }

    @Test
    fun givenText_whenCheckedForSevenBit_thenOnlyConvertibleScriptsPass() {
        assertTrue(simple.canConvertAll("Olá “amigo” ŧ"))
        assertFalse(simple.canConvertAll("hi 👋"))
        assertFalse(simple.canConvertAll("Привет"))
    }
}
