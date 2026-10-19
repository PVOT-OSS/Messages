/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

import androidx.lifecycle.LifecycleOwner

interface QkViewContract<in State> : LifecycleOwner {

    fun render(state: State)
}
