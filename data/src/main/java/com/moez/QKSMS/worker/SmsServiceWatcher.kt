/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.worker

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import timber.log.Timber

object SmsServiceWatcher {

    private var stop: (() -> Unit)? = null

    fun watch(context: Context) {
        val appContext = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            if (stop != null) return@post
            var hadService: Boolean? = null
            try {
                stop =
                    register(appContext) { inService ->
                        val regained = hadService == false && inService
                        hadService = inService
                        if (!regained) return@register
                        SmsSendRetryWorker.kick(appContext, resetDelays = false)
                    }
            } catch (e: Exception) {
                Timber.w(e, "Can't watch the service state")
            }
        }
    }

    fun unwatch() {
        Handler(Looper.getMainLooper()).post {
            stop?.invoke()
            stop = null
        }
    }

    private fun register(context: Context, onChanged: (Boolean) -> Unit): () -> Unit {
        val telephony = context.getSystemService(TelephonyManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback =
                object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
                    override fun onServiceStateChanged(serviceState: ServiceState) {
                        onChanged(serviceState.state == ServiceState.STATE_IN_SERVICE)
                    }
                }
            telephony.registerTelephonyCallback(context.mainExecutor, callback)
            return { telephony.unregisterTelephonyCallback(callback) }
        }

        @Suppress("DEPRECATION")
        val listener =
            object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onServiceStateChanged(serviceState: ServiceState) {
                    onChanged(serviceState.state == ServiceState.STATE_IN_SERVICE)
                }
            }
        @Suppress("DEPRECATION") telephony.listen(listener, PhoneStateListener.LISTEN_SERVICE_STATE)
        @Suppress("DEPRECATION")
        return {
            telephony.listen(listener, PhoneStateListener.LISTEN_NONE)
        }
    }
}
