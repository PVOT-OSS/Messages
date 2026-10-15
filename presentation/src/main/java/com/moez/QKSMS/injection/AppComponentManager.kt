/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.injection

import org.prauga.messages.common.QKApplication

internal lateinit var appComponent: AppComponent
    private set

internal object AppComponentManager {

    fun init(application: QKApplication) {
        appComponent = DaggerAppComponent.builder().appModule(AppModule(application)).build()
    }
}
