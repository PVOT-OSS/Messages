/*
 * Copyright (C) 2025 Saalim Quadri <danascape@gmail.com>
 */
package com.klinker.android.send_message

import com.google.android.mms.ContentType

object SmilBuilder {

    private const val DURATION = "5000ms"

    data class Part(val contentType: String, val location: String)

    fun build(parts: List<Part>): String {
        val hasText = parts.any { it.contentType.isText() }
        val hasVisual = parts.any { it.contentType.isVisual() }

        val regions =
            when {
                hasVisual && hasText ->
                    region("Image", top = 0, height = 80) + region("Text", top = 80, height = 20)
                hasVisual -> region("Image", top = 0, height = 100)
                hasText -> region("Text", top = 0, height = 100)
                else -> ""
            }

        val body =
            parts.joinToString("") { part ->
                val src = part.location.escape()
                val element =
                    when {
                        part.contentType.isText() -> "<text src=\"$src\" region=\"Text\"/>"
                        ContentType.isImageType(part.contentType) ->
                            "<img src=\"$src\" region=\"Image\"/>"
                        ContentType.isVideoType(part.contentType) ->
                            "<video src=\"$src\" region=\"Image\"/>"
                        ContentType.isAudioType(part.contentType) -> "<audio src=\"$src\"/>"
                        else -> "<ref src=\"$src\"/>"
                    }
                "<par dur=\"$DURATION\">$element</par>"
            }

        return "<smil><head><layout><root-layout/>$regions</layout></head><body>$body</body></smil>"
    }

    private fun region(id: String, top: Int, height: Int) =
        "<region id=\"$id\" top=\"$top%\" left=\"0%\" height=\"$height%\" width=\"100%\" " +
            "fit=\"${if (id == "Text") "scroll" else "meet"}\"/>"

    private fun String.isText() = this == ContentType.TEXT_PLAIN || this == ContentType.TEXT_HTML

    private fun String.isVisual() =
        ContentType.isImageType(this) || ContentType.isVideoType(this)

    private fun String.escape() =
        replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
}
