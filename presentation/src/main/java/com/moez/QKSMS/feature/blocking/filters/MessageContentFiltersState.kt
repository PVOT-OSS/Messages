/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.feature.blocking.filters

import io.realm.RealmResults
import org.prauga.messages.model.MessageContentFilter

data class MessageContentFiltersState(val filters: RealmResults<MessageContentFilter>? = null)
