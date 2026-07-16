package com.tecnozoni.reproductor.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Servicio de reproducción. Corre independiente de la pantalla, por eso la música
 * sigue sonando en background. MediaSessionService de Media3 aporta solo:
 * notificación con controles, botones de auriculares/bluetooth y foreground service.
 *
 * La UI NO usa este servicio directo: se conecta con un MediaController (ver PlaybackController).
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            // handleAudioFocus = true: pausa solo si entra una llamada u otra app toma el audio,
            // y baja el volumen ("ducking") ante notificaciones. Manejo de foco gratis.
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            // Pausa si se desconectan los auriculares, para no reventar por el parlante.
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    // Si el usuario cierra la app desde "recientes" y no hay nada sonando, frenamos el servicio.
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
