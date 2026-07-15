package com.tecnozoni.reproductor.data.model

import android.net.Uri

/**
 * Modelo de dominio de una canción. Es lo que la UI conoce.
 *
 * @param id       el _ID de MediaStore (estable por canción; lo usaremos como PK
 *                 del orden custom en Room más adelante).
 * @param uri      content:// URI para reproducir/abrir la canción (ExoPlayer la consume).
 * @param durationMs      duración en milisegundos.
 * @param dateModifiedSec fecha de modificación en segundos epoch (como la da MediaStore).
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val dateModifiedSec: Long,
    val uri: Uri,
)
