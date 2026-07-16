package com.tecnozoni.reproductor.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.tecnozoni.reproductor.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Estado de reproducción que observa la UI (lo mínimo para el mini-player en Hito 2). */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTitle: String? = null,
    val currentArtist: String? = null,
    val hasCurrent: Boolean = false,
)

/**
 * "Control remoto" hacia el PlaybackService. Construye un MediaController (conexión
 * asíncrona) y expone el estado como StateFlow para Compose. Singleton: una sola
 * conexión para toda la app.
 */
@Singleton
class PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    // Acción encolada si el usuario toca "play" antes de que el controller termine de conectar.
    private var pendingAction: ((MediaController) -> Unit)? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = updateState()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()
        override fun onPlaybackStateChanged(playbackState: Int) = updateState()
    }

    /** Conecta con el servicio. Idempotente (se llama al crear el ViewModel). */
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
                updateState()
            },
            // Los callbacks del controller van en el main thread.
            ContextCompat.getMainExecutor(context),
        )
    }

    /** Carga la lista como cola y empieza a reproducir desde startIndex. */
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

    private fun runOrPend(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) action(c) else pendingAction = action
    }

    private fun updateState() {
        val c = controller ?: return
        val metadata = c.currentMediaItem?.mediaMetadata
        _state.value = PlaybackState(
            isPlaying = c.isPlaying,
            currentTitle = metadata?.title?.toString(),
            currentArtist = metadata?.artist?.toString(),
            hasCurrent = c.currentMediaItem != null,
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
