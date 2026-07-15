package com.tecnozoni.reproductor.data

import com.tecnozoni.reproductor.data.mediastore.MediaStoreSource
import com.tecnozoni.reproductor.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Punto único de acceso a datos de canciones para la UI/ViewModel.
 *
 * Hito 1: solo lee de MediaStore. En el Hito 4 combinará MediaStore + Room
 * (orden custom). El ViewModel nunca habla directo con MediaStore ni Room.
 */
class SongRepository @Inject constructor(
    private val mediaStoreSource: MediaStoreSource,
) {

    /** Lee todas las canciones. Corre en IO para no bloquear el main thread. */
    suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        mediaStoreSource.querySongs()
    }
}
