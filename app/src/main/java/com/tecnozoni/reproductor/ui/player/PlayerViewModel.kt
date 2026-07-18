package com.tecnozoni.reproductor.ui.player

import androidx.lifecycle.ViewModel
import com.tecnozoni.reproductor.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel de la pantalla de reproducción completa. Es delgado: solo delega en el
 * PlaybackController (singleton), así comparte el MISMO estado que la lista y el
 * mini-player — todo queda sincronizado.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    val playbackState = playbackController.state
    val queue = playbackController.queue

    fun playIndex(index: Int) = playbackController.playIndex(index)
    fun togglePlayPause() = playbackController.togglePlayPause()
    fun next() = playbackController.next()
    fun previous() = playbackController.previous()
    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)
    fun toggleShuffle() = playbackController.toggleShuffle()
    fun cycleRepeat() = playbackController.cycleRepeat()
}
