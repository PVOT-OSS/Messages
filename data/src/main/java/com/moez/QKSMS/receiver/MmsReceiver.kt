/*
 * SPDX-FileCopyrightText: 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.receiver

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.mms.transaction.PushReceiver
import dagger.android.AndroidInjection
import javax.inject.Inject
import org.prauga.messages.carrier.CarrierMmsConfig
import org.prauga.messages.interactor.ReceiveMms
import org.prauga.messages.manager.MmsDownloadPolicy
import timber.log.Timber

open class MmsReceiver : PushReceiver() {

    @Inject lateinit var mmsDownloadPolicy: MmsDownloadPolicy
    @Inject lateinit var receiveMms: ReceiveMms

    override fun onReceive(context: Context, intent: Intent) {
        inject(context)
        super.onReceive(context, intent)
    }

    protected open fun inject(context: Context) {
        AndroidInjection.inject(this, context)
    }

    override fun shouldAutoDownload(
        context: Context,
        subId: Int,
        config: CarrierMmsConfig,
        from: String?,
    ): Boolean = mmsDownloadPolicy.shouldAutoDownload(subId, from)

    override fun onNotificationReceived(context: Context, notificationUri: Uri, subId: Int) {
        receiveMms.buildObservable(notificationUri).blockingSubscribe({}, Timber::w)
    }
}
