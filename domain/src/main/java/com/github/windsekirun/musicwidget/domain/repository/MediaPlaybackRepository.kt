package com.github.windsekirun.musicwidget.domain.repository

import com.github.windsekirun.musicwidget.domain.model.MediaPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface MediaPlaybackRepository {
    val playbackState: StateFlow<MediaPlaybackState>

    fun playPause()
    fun skipToNext()
    fun skipToPrevious()
    fun seekTo(positionMs: Long)
    fun updatePlaybackState(state: MediaPlaybackState)
    fun updatePosition(positionMs: Long)
    fun setActionHandler(handler: ActionHandler?)
    fun reset()

    fun setActions(
        playPause: (() -> Unit)? = null,
        skipToNext: (() -> Unit)? = null,
        skipToPrevious: (() -> Unit)? = null,
        seekTo: ((Long) -> Unit)? = null
    ) {
        setActionHandler(object : ActionHandler {
            override fun onPlayPause() {
                playPause?.invoke()
            }

            override fun onSkipToNext() {
                skipToNext?.invoke()
            }

            override fun onSkipToPrevious() {
                skipToPrevious?.invoke()
            }

            override fun onSeekTo(positionMs: Long) {
                seekTo?.invoke(positionMs)
            }
        })
    }

    interface ActionHandler {
        fun onPlayPause() {}
        fun onSkipToNext() {}
        fun onSkipToPrevious() {}
        fun onSeekTo(positionMs: Long) {}
    }

    companion object : DefaultMediaPlaybackRepository()
}

open class DefaultMediaPlaybackRepository : MediaPlaybackRepository {
    protected val _playbackState = MutableStateFlow(MediaPlaybackState())
    override val playbackState: StateFlow<MediaPlaybackState> = _playbackState.asStateFlow()

    @Volatile
    private var actionHandler: MediaPlaybackRepository.ActionHandler? = null

    override fun setActionHandler(handler: MediaPlaybackRepository.ActionHandler?) {
        this.actionHandler = handler
    }

    override fun playPause() {
        actionHandler?.onPlayPause()
    }

    override fun skipToNext() {
        actionHandler?.onSkipToNext()
    }

    override fun skipToPrevious() {
        actionHandler?.onSkipToPrevious()
    }

    override fun seekTo(positionMs: Long) {
        actionHandler?.onSeekTo(positionMs)
    }

    override fun updatePlaybackState(state: MediaPlaybackState) {
        _playbackState.value = state
    }

    override fun updatePosition(positionMs: Long) {
        _playbackState.update { it.copy(positionMs = positionMs) }
    }

    override fun reset() {
        actionHandler = null
        _playbackState.value = MediaPlaybackState()
    }
}
