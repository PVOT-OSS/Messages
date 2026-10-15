/*
 * SPDX-FileCopyrightText: 2023 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.util

import android.net.Uri
import com.f2prateek.rx.preferences2.Preference.Converter

class UriPreferenceConverter : Converter<Uri> {

    override fun deserialize(serialized: String): Uri {
        return Uri.parse(serialized)
    }

    override fun serialize(value: Uri): String {
        return value.toString()
    }
}
