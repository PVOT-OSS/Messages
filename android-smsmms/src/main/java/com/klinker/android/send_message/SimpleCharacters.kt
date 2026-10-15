/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.klinker.android.send_message

import android.content.Context
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber

class SimpleCharacters(private val table: Map<Char, Char>) {

    fun convert(text: String): String =
        buildString(text.length) {
            text.forEach { c ->
                append(
                    when {
                        keeps(c) -> c
                        !convertible(c) -> c
                        else -> table[c] ?: ' '
                    }
                )
            }
        }

    fun canConvertAll(text: String): Boolean = text.all { keeps(it) || convertible(it) }

    private fun keeps(c: Char) =
        (c in ' '..'~' && c != '`') || c == '\n' || c == '\u000c' || c == '\r' || c in GSM_ALPHABET

    private fun convertible(c: Char): Boolean {
        if (Character.isSurrogate(c)) return false
        return when (Character.UnicodeScript.of(c.code)) {
            Character.UnicodeScript.LATIN,
            Character.UnicodeScript.GREEK,
            Character.UnicodeScript.COMMON,
            Character.UnicodeScript.INHERITED -> true
            else -> false
        }
    }

    companion object {
        private const val GSM_ALPHABET = "£¥èéùìòÇØøÅåΔΦΓΛΩΠΨΣΘΞÆæßÉ¤¡ÄÖÑÜ§¿äöñüà€"

        @Volatile private var instance: SimpleCharacters? = null

        fun get(context: Context): SimpleCharacters =
            instance
                ?: SimpleCharacters(
                        load(context.resources.getXml(R.xml.sms_7bit_translation_table))
                    )
                    .also { instance = it }

        private fun load(parser: XmlResourceParser): Map<Char, Char> {
            val table = HashMap<Char, Char>()
            try {
                var type: String? = null
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType != XmlPullParser.START_TAG) continue
                    when (parser.name) {
                        "TranslationType" -> type = parser.getAttributeValue(null, "Type")
                        "Character" ->
                            if (type == "common" || type == "gsm") {
                                val from = parser.getAttributeValue(null, "from").decode()
                                val to = parser.getAttributeValue(null, "to").decode()
                                if (from != null && to != null) table[from] = to
                            }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Unable to read the 7-bit translation table")
            } finally {
                parser.close()
            }
            return table
        }

        private fun String?.decode(): Char? = this?.removePrefix("0x")?.toIntOrNull(16)?.toChar()
    }
}
