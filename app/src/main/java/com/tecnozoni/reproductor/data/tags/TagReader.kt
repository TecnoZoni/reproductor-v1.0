package com.tecnozoni.reproductor.data.tags

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Lee los tags ID3 CRUDOS directamente del archivo, respetando la codificación con
 * la que fueron escritos. Sirve para recuperar títulos/artistas que MediaStore
 * devuelve rotos (mojibake), porque MediaStore ya los decodificó mal y perdió el dato,
 * pero el tag original sigue intacto dentro del archivo.
 *
 * Es best-effort: los ID3v2 mal escaneados se recuperan bien; algunos ID3v1 viejos
 * en codificaciones locales (cirílico, CJK) pueden no recuperarse.
 */
object TagReader {
    init {
        // jaudiotagger loguea muchísimo vía java.util.logging; lo apagamos.
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    data class Tags(val title: String?, val artist: String?)

    /** Devuelve title/artist del archivo, o null si no se pudo leer. */
    fun read(path: String): Tags? {
        return try {
            val file = File(path)
            if (!file.isFile) return null
            val tag = AudioFileIO.read(file).tag ?: return null
            Tags(
                title = tag.getFirst(FieldKey.TITLE)?.trim()?.ifBlank { null },
                artist = tag.getFirst(FieldKey.ARTIST)?.trim()?.ifBlank { null },
            )
        } catch (e: Exception) {
            null
        }
    }
}
