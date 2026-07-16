package com.tecnozoni.reproductor.ui.songlist.components

import android.content.Context
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tecnozoni.reproductor.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

// Caché en memoria del arte ya cargado (evita releer el archivo al scrollear).
private val artworkCache = LruCache<Long, ImageBitmap>(200)
// IDs que ya sabemos que NO tienen arte, para no reintentar en cada recomposición.
private val noArtworkIds = Collections.synchronizedSet(mutableSetOf<Long>())

/**
 * Muestra la carátula embebida de la canción, o un placeholder por default si no tiene.
 * Carga async con loadThumbnail (API 29+), que extrae el arte embebido del archivo.
 */
@Composable
fun SongArtwork(song: Song, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val artwork by produceState<ImageBitmap?>(
        initialValue = artworkCache.get(song.id),
        key1 = song.id,
    ) {
        if (value == null && song.id !in noArtworkIds) {
            value = loadArtwork(context, song.uri, song.id)
        }
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val art = artwork
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp),
            )
        } else {
            // Placeholder por default: nota musical sobre fondo neutro.
            Text(
                text = "♪",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private suspend fun loadArtwork(context: Context, uri: Uri, id: Long): ImageBitmap? =
    withContext(Dispatchers.IO) {
        try {
            val image = context.contentResolver
                .loadThumbnail(uri, Size(128, 128), null)
                .asImageBitmap()
            artworkCache.put(id, image)
            image
        } catch (e: Exception) {
            // Sin arte embebido (o el device no lo pudo extraer): lo marcamos y mostramos placeholder.
            noArtworkIds.add(id)
            null
        }
    }
