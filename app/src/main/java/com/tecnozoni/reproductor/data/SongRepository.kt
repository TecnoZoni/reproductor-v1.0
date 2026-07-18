package com.tecnozoni.reproductor.data

import com.tecnozoni.reproductor.data.local.CustomOrderDao
import com.tecnozoni.reproductor.data.local.CustomOrderEntity
import com.tecnozoni.reproductor.data.mediastore.MediaStoreSource
import com.tecnozoni.reproductor.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Punto único de acceso a datos de canciones para la UI/ViewModel.
 *
 * MediaStore = fuente de verdad de QUÉ canciones existen.
 * Room (custom_order) = SOLO el orden personalizado (id -> posición).
 */
class SongRepository @Inject constructor(
    private val mediaStoreSource: MediaStoreSource,
    private val customOrderDao: CustomOrderDao,
) {

    suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        mediaStoreSource.querySongs()
    }

    /** Mapa id -> posición del orden personalizado guardado. */
    suspend fun getCustomOrder(): Map<Long, Int> = withContext(Dispatchers.IO) {
        customOrderDao.getAll().associate { it.mediaId to it.position }
    }

    /** Guarda el orden personalizado: la posición es el índice en la lista. */
    suspend fun saveCustomOrder(orderedIds: List<Long>) = withContext(Dispatchers.IO) {
        val items = orderedIds.mapIndexed { index, id -> CustomOrderEntity(mediaId = id, position = index) }
        customOrderDao.replaceAll(items)
    }
}
