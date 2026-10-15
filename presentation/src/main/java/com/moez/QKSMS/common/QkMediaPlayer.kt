/*
 * SPDX-FileCopyrightText: 2025 gavine99 <github@xymail.tk>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.moez.QKSMS.common

import android.media.MediaPlayer

// singleton class for a media player that can be re-used throughout the app
// because we probably don't ever want to play multiple media files at the
// same time
object QkMediaPlayer : MediaPlayer() {
    private var onCompletionListener: OnCompletionListener? = null

    enum class PlayingState {
        None,
        Stopped,
        Playing,
        Paused,
    }

    private var playingState = PlayingState.Stopped

    override fun start() {
        playingState = PlayingState.Playing
        super.start()
    }

    override fun pause() {
        playingState = PlayingState.Paused
        super.pause()
    }

    override fun reset() {
        if (playingState != PlayingState.Stopped) stop()
        onCompletionListener = null
        super.reset()
    }

    override fun stop() {
        playingState = PlayingState.Stopped
        super.stop()
        onCompletionListener?.onCompletion(this) // and manually call on completion handler
    }

    override fun setOnCompletionListener(listener: OnCompletionListener?) {
        onCompletionListener = listener
        super.setOnCompletionListener(listener)
    }
}
