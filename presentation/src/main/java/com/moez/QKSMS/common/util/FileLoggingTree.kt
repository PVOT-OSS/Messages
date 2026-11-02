/*
 * SPDX-FileCopyrightText: 2018 Moez Bhatti <moez.bhatti@gmail.com>
 * SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.prauga.messages.common.util

import android.content.Context
import android.net.Uri
import android.util.Log
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import org.prauga.messages.util.FileUtils
import org.prauga.messages.util.Preferences
import timber.log.Timber

/**
 * Based off Vipin Kumar's FileLoggingTree:
 * https://medium.com/@vicky7230/file-logging-with-timber-4e63a1b86a66
 */
@Singleton
class FileLoggingTree
@Inject
constructor(private val prefs: Preferences, private val context: Context) : Timber.DebugTree() {
    companion object {
        val TAG: String? = FileLoggingTree::class.simpleName
        const val MAX_PENDING_WRITES = 1_000
    }

    private var logFileUri: Uri? = null

    private val writer =
        ThreadPoolExecutor(
                1,
                1,
                30,
                TimeUnit.SECONDS,
                ArrayBlockingQueue(MAX_PENDING_WRITES),
                { runnable -> Thread(runnable, "FileLoggingTree") },
                ThreadPoolExecutor.DiscardOldestPolicy(),
            )
            .apply { allowCoreThreadTimeOut(true) }

    internal val pendingWrites: Int
        get() = writer.queue.size

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!prefs.logging.get()) return

        val time = System.currentTimeMillis()
        writer.execute {
            run {
                val logItem =
                    "${    // date/time
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss:SSS",
                            Locale.getDefault(),
                        ).format(time)
                    } ${    // priority
                        when (priority) {
                            Log.VERBOSE -> "V"
                            Log.DEBUG -> "D"
                            Log.INFO -> "I"
                            Log.WARN -> "W"
                            Log.ERROR -> "E"
                            else -> "?"
                        }
                    }/${    // tag
                        tag
                    }: ${    // message
                        message
                    }${    // stack trace
                        Log.getStackTraceString(t)
                    }\n"

                // if uri of log file not yet determined, get one now
                if (logFileUri == null) {
                    val filename =
                        "Quik-log-${
                        SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault(),
                        ).format(System.currentTimeMillis())
                    }.log"

                    val (uri, e) =
                        FileUtils.create(
                            FileUtils.Companion.Location.Downloads,
                            context,
                            filename,
                            "text/plain",
                        )
                    if (e is Exception) Log.e(TAG, "Error opening log file", e)
                    else logFileUri = uri
                }

                logFileUri?.let {
                    val e = FileUtils.append(context, it, logItem.toByteArray())
                    if (e is FileNotFoundException)
                        Log.e(TAG, "Log file went away. Lost log file item: $logItem", e)
                    else if (e is Exception) Log.e(TAG, "Error while logging into file", e)
                }
            }
        }
    }
}

fun FileLoggingTree.plantWhile(enabled: Observable<Boolean>): Disposable =
    enabled.distinctUntilChanged().subscribe { on ->
        val planted = this in Timber.forest()
        if (on && !planted) Timber.plant(this) else if (!on && planted) Timber.uproot(this)
    }
