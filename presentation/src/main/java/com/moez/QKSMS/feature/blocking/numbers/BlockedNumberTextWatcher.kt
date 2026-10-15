/*
 * SPDX-FileCopyrightText: 2019 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.numbers

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import org.prauga.messages.util.PhoneNumberUtils

class BlockedNumberTextWatcher(
    private val editText: EditText,
    private val phoneNumberUtils: PhoneNumberUtils,
) : TextWatcher {

    init {
        editText.addTextChangedListener(this)
    }

    override fun afterTextChanged(s: Editable?) {
        editText.removeTextChangedListener(this)

        val formatted = s?.let(phoneNumberUtils::formatNumber)
        if (s?.toString() != formatted && formatted != null) {
            editText.setText(formatted)
            editText.setSelection(formatted.length)
        }

        editText.addTextChangedListener(this)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    fun dispose() {
        editText.removeTextChangedListener(this)
    }
}
