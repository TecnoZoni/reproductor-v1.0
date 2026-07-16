package com.tecnozoni.reproductor.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.tecnozoni.reproductor.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Lee las canciones del dispositivo desde MediaStore (scoped storage).
 *
 * MediaStore = base de datos del sistema que indexa el multimedia. Consultamos con
 * ContentResolver.query(...) que devuelve un Cursor (similar a un ResultSet de JDBC):
 * se recorre fila por fila. Requiere permiso de lectura de audio concedido.
 *
 * @ApplicationContext: Hilt inyecta el Context de la Application (no el de una
 * Activity), que es el correcto para algo que vive mientras vive la app.
 */
class MediaStoreSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** Query bloqueante — llamar siempre desde un dispatcher de IO (lo hace el Repository). */
    fun querySongs(): List<Song> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME, // nombre de archivo, fallback si no hay TITLE
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )

        // Solo música (excluye tonos, notificaciones, grabaciones, etc.).
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val songs = mutableListOf<Song>()

        context.contentResolver.query(
            collection,
            projection,
            selection,
            /* selectionArgs = */ null,
            /* sortOrder = */ null, // ordenamos en memoria (al vuelo) en el ViewModel
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)

                // TITLE puede venir null/vacío, o "roto" (mojibake: tags en codificación
                // vieja que Android decodificó mal → contiene el carácter '�' �).
                // En cualquiera de esos casos caemos al nombre de archivo, y si no, a genérico.
                val rawTitle = cursor.getString(titleCol)?.takeIf { it.isUsable() }
                val fileName = cursor.getString(displayNameCol)?.substringBeforeLast('.')
                val title = rawTitle ?: fileName?.takeIf { it.isNotBlank() } ?: "(sin título)"

                // MediaStore usa el literal "<unknown>" cuando no conoce el artista.
                val artist = cursor.getString(artistCol)
                    ?.takeIf { it.isUsable() && it != MediaStore.UNKNOWN_STRING }
                    ?: "Artista desconocido"

                songs += Song(
                    id = id,
                    title = title,
                    artist = artist,
                    durationMs = cursor.getLong(durationCol),
                    dateModifiedSec = cursor.getLong(dateCol),
                    uri = ContentUris.withAppendedId(collection, id),
                )
            }
        }

        return songs
    }
}

/** true si el texto sirve para mostrar: no vacío y sin el carácter de reemplazo '�' (mojibake). */
private fun String.isUsable(): Boolean = isNotBlank() && !contains('�')
