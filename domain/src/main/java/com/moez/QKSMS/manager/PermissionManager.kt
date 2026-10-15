/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

interface PermissionManager {

    fun isDefaultSms(): Boolean

    fun hasReadSms(): Boolean

    fun hasSendSms(): Boolean

    fun hasContacts(): Boolean

    fun hasNotifications(): Boolean

    fun hasPhone(): Boolean

    fun hasCalling(): Boolean

    fun hasStorage(): Boolean

    fun hasRecordAudio(): Boolean

    fun hasExactAlarms(): Boolean
}
