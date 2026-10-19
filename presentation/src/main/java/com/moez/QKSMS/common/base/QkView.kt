/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.base

import androidx.lifecycle.LifecycleOwner

interface QkView<in State> : LifecycleOwner {

    fun render(state: State)
}
