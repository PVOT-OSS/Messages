/*
 * SPDX-FileCopyrightText: 2021 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util.extensions

import ezvcard.VCard

fun VCard.getDisplayName(): String? {
    return formattedName?.value
        ?: telephoneNumbers?.firstOrNull()?.text
        ?: emails?.firstOrNull()?.value
}
