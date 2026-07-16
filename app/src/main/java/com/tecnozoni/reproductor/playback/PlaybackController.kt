package com.tecnozoni.reproductor.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.tecnozoni.reproductor.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Estado de reproducción que observa la UI. */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTitle: String? = null,
    val currentArtist: String? = null,
    val hasCurrent: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

/**
 * "Control remoto" hacia el PlaybackService. Construye un MediaController (conexión
 * asíncrona) y expone el estado como StateFlow para Compose. Singleton: una sola
 * conexión para toda la app.
 *
 * La posición (segundo actual) no llega por eventos: se consulta por polling cada
 * 500ms mientras suena, para mover la barra de progreso.
 */
@Singleton
class PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingAction: ((MediaController) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionJob: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) startPositionUpdates() else stopPositionUpdates()
            updateState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()
        override fun onPlaybackStateChanged(playbackState: Int) = updateState()
    }

    fun initialize() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                pendingAction?.let { action -> action(c) }
                pendingAction = null
                if (c.isPlaying) startPositionUpdates()
                updateState()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun playSongs(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val items = songs.map { it.toMediaItem() }
        runOrPend { c ->
            c.setMediaItems(items, startIndex, 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        updateState()
    }

    private fun runOrPend(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) action(c) else pendingAction = action
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                updateState()
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun updateState() {
        val c = controller ?: return
        val metadata = c.currentMediaItem?.mediaMetadata
        val rawDuration = c.duration
        _state.value = PlaybackState(
            isPlaying = c.isPlaying,
            currentTitle = metadata?.title?.toString(),
            currentArtist = metadata?.artist?.toString(),
            hasCurrent = c.currentMediaItem != null,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = if (rawDuration == C.TIME_UNSET || rawDuration < 0) 0L else rawDuration,
        )
    }
}

/** Song del dominio -> MediaItem de Media3. El mediaId es el _ID de MediaStore. */
private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .build(),
    )
    .build()
