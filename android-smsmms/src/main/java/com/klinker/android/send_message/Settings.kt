/*
 * SPDX-FileCopyrightText: 2013 Jacob Klinker
 * SPDX-License-Identifier: Apache-2.0
 */

package com.klinker.android.send_message

/** Class to house all of the settings that can be used to send a message */
class Settings
@JvmOverloads
constructor(
    // MMS options
    var mmsc: String? = "",
    var proxy: String? = "",
    var port: String? = "0",
    var agent: String? = "",
    var userProfileUrl: String? = "",
    var uaProfTagName: String? = "",

    // SMS options
    var stripUnicode: Boolean = false,
)
