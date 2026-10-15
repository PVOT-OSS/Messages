/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.manager

import android.content.Context
import com.android.mms.transaction.PushReceiver
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.prauga.messages.blocking.BlockingClient
import org.prauga.messages.carrier.CarrierMmsConfig
import org.prauga.messages.util.Preferences

class MmsDownloadPolicyImpl
@Inject
constructor(
    private val context: Context,
    private val blockingClient: BlockingClient,
    private val prefs: Preferences,
) : MmsDownloadPolicy {

    override fun shouldAutoDownload(subId: Int, from: String?): Boolean {
        val config = CarrierMmsConfig.get(context, subId)

        val autoDownload =
            if (prefs.mmsAutoDownload.isSet) prefs.mmsAutoDownload.get()
            else config.autoRetrieveEnabled
        if (!autoDownload) return false

        if (PushReceiver.isRoaming(context, subId)) {
            val roamingAutoDownload =
                if (prefs.mmsRoamingAutoDownload.isSet) prefs.mmsRoamingAutoDownload.get()
                else config.roamingAutoRetrieveEnabled
            if (!roamingAutoDownload) return false
        }

        return from.isNullOrEmpty() || !isBlocked(from)
    }

    private fun isBlocked(address: String): Boolean =
        blockingClient
            .shouldBlock(address)
            .timeout(5, TimeUnit.SECONDS)
            .onErrorReturnItem(BlockingClient.Action.DoNothing)
            .blockingGet() is BlockingClient.Action.Block
}
