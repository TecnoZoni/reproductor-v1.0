package com.tecnozoni.reproductor.ui.songlist.components

import android.content.Context
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tecnozoni.reproductor.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

// Caché en memoria del arte chico (fila) ya cargado (evita releer al scrollear).
private val artworkCache = LruCache<Long, ImageBitmap>(200)
// IDs que ya sabemos que NO tienen arte, para no reintentar en cada recomposición.
private val noArtworkIds = Collections.synchronizedSet(mutableSetOf<Long>())

/** Carátula chica para las filas de la lista (48dp, con caché). */
@Composable
fun SongArtwork(song: Song, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val artwork by produceState<ImageBitmap?>(
        initialValue = artworkCache.get(song.id),
        key1 = song.id,
    ) {
        if (value == null && song.id !in noArtworkIds) {
            val bitmap = decodeThumbnail(context, song.uri, sizePx = 128)
            if (bitmap != null) artworkCache.put(song.id, bitmap) else noArtworkIds.add(song.id)
            value = bitmap
        }
    }

    ArtBox(
        image = artwork,
        cornerRadius = 6.dp,
        placeholderStyle = MaterialTheme.typography.titleLarge,
        modifier = modifier.size(48.dp),
    )
}

/** Carátula grande para la pantalla de reproducción completa (sin caché, 1 por canción). */
@Composable
fun PlayerArtwork(uri: Uri?, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val artwork by produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        value = if (uri != null) decodeThumbnail(context, uri, sizePx = 512) else null
    }

    ArtBox(
        image = artwork,
        cornerRadius = 16.dp,
        placeholderStyle = MaterialTheme.typography.displayLarge,
        modifier = modifier,
    )
}

@Composable
private fun ArtBox(
    image: ImageBitmap?,
    cornerRadius: Dp,
    placeholderStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = "♪",
                style = placeholderStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Extrae el arte embebido del archivo vía loadThumbnail (API 29+). null si no tiene. */
private suspend fun decodeThumbnail(context: Context, uri: Uri, sizePx: Int): ImageBitmap? =
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver
                .loadThumbnail(uri, Size(sizePx, sizePx), null)
                .asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
