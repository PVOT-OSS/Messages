/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.gallery

import io.realm.RealmResults
import org.prauga.messages.model.MmsPart

data class GalleryState(
    val navigationVisible: Boolean = true,
    val title: String? = "",
    val parts: RealmResults<MmsPart>? = null,
)
