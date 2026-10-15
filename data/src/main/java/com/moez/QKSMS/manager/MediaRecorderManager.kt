/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.moez.QKSMS.manager

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.core.net.toFile
import java.util.UUID
import org.prauga.messages.common.util.FileNamingConfig
import org.prauga.messages.util.FileUtils

object MediaRecorderManager : MediaRecorder() {

    enum class RecordingState {
        Initial,
        DataSourceConfigured,
        Prepared,
        Recording,
        Error,
    }

    const val AUDIO_FILE_PREFIX = FileNamingConfig.AUDIO_FILE_PREFIX
    const val AUDIO_FILE_SUFFIX = FileNamingConfig.AUDIO_FILE_SUFFIX

    var recordingState: RecordingState = RecordingState.Initial
        private set

    var uri: Uri = Uri.EMPTY
        private set

    fun stopRecording(): Uri {
        return try {
            if (recordingState == RecordingState.Recording) stop()

            reset()
            recordingState = RecordingState.Initial

            uri
        } catch (e: Exception) {
            Uri.EMPTY
        }
    }

    fun startRecording(context: Context, preferredAudioDevice: AudioDeviceInfo? = null): Uri {
        return try {
            val (newUri, e) =
                FileUtils.create(
                    FileUtils.Companion.Location.Cache,
                    context,
                    "$AUDIO_FILE_PREFIX${UUID.randomUUID()}$AUDIO_FILE_SUFFIX",
                    "",
                )
            if (e is Exception) throw e

            uri = newUri

            // ensure stopped before using again
            stopRecording()

            // configure
            setAudioSource(AudioSource.MIC)
            setOutputFormat(OutputFormat.THREE_GPP)
            setAudioEncoder(AudioEncoder.AMR_WB)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                preferredDevice = preferredAudioDevice

            recordingState = RecordingState.DataSourceConfigured

            setOutputFile(uri.toFile().path)

            prepare()
            recordingState = RecordingState.Prepared

            start()
            recordingState = RecordingState.Recording

            uri
        } catch (e: Exception) {
            Uri.EMPTY
        }
    }
}
