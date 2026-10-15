/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.extensions

import com.google.android.mms.ContentType
import org.prauga.messages.model.MmsPart

fun MmsPart.isSmil() = ContentType.APP_SMIL.lowercase() == type.lowercase()

fun MmsPart.isImage() = ContentType.isImageType(type.lowercase())

fun MmsPart.isVideo() = ContentType.isVideoType(type.lowercase())

fun MmsPart.isAudio() = ContentType.isAudioType(type.lowercase())

fun MmsPart.isText() = ContentType.TEXT_PLAIN.lowercase() == type.lowercase()

fun MmsPart.isVCard() = ContentType.TEXT_VCARD.lowercase() == type.lowercase()
