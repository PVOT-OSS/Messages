/*
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.klinker.android.send_message

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TelephonyNetworkSpecifier
import android.os.Build
import androidx.core.content.getSystemService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import timber.log.Timber

class MmsNetworkProbe(private val context: Context) {

    enum class Result {
        AVAILABLE,
        UNAVAILABLE,
        UNKNOWN,
    }

    fun await(subId: Int, timeoutMillis: Long): Result {
        val connectivity = context.getSystemService<ConnectivityManager>() ?: return Result.UNKNOWN
        val latch = CountDownLatch(1)
        var result = Result.UNAVAILABLE
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    result = Result.AVAILABLE
                    latch.countDown()
                }

                override fun onUnavailable() {
                    latch.countDown()
                }
            }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                connectivity.requestNetwork(request(subId), callback, timeoutMillis.toInt())
            } else {
                connectivity.requestNetwork(request(subId), callback)
            }
        } catch (e: RuntimeException) {
            Timber.w(e, "Unable to request an MMS network for sub $subId")
            return Result.UNKNOWN
        }

        try {
            latch.await(timeoutMillis + RELEASE_GRACE_MILLIS, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            result = Result.UNKNOWN
        } finally {
            try {
                connectivity.unregisterNetworkCallback(callback)
            } catch (e: IllegalArgumentException) {
                // Already released by the timeout
            }
        }
        return result
    }

    private fun request(subId: Int): NetworkRequest =
        NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
            .apply {
                if (subId < 0) return@apply
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setNetworkSpecifier(
                        TelephonyNetworkSpecifier.Builder().setSubscriptionId(subId).build()
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    @Suppress("DEPRECATION") setNetworkSpecifier(subId.toString())
                }
            }
            .build()

    private companion object {
        const val RELEASE_GRACE_MILLIS = 2_000L
    }
}
