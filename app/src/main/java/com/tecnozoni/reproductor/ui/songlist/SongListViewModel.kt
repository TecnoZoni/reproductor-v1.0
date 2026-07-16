package com.tecnozoni.reproductor.ui.songlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecnozoni.reproductor.data.SettingsRepository
import com.tecnozoni.reproductor.data.SongRepository
import com.tecnozoni.reproductor.data.model.Song
import com.tecnozoni.reproductor.data.model.SortOrder
import com.tecnozoni.reproductor.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado inmutable que la UI observa y dibuja. */
data class SongListUiState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val sort: SortOrder = SortOrder.NAME,
    val loaded: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel de la pantalla única. @HiltViewModel + constructor @Inject: Hilt le
 * arma el SongRepository solo. Sobrevive a rotaciones de pantalla.
 *
 * Guarda la lista completa una vez (allSongs) y reordena EN MEMORIA al cambiar el
 * criterio — no re-consulta MediaStore. Eso es "ordenar al vuelo".
 */
@HiltViewModel
class SongListViewModel @Inject constructor(
    private val repository: SongRepository,
    private val settingsRepository: SettingsRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongListUiState())
    val uiState: StateFlow<SongListUiState> = _uiState.asStateFlow()

    /** Estado de reproducción para el mini-player. */
    val playbackState = playbackController.state

    private var allSongs: List<Song> = emptyList()
    // Orden personalizado guardado: id -> posición.
    private var customOrder: Map<Long, Int> = emptyMap()
    private var saveOrderJob: Job? = null

    init {
        // Conecta la UI con el servicio de reproducción (idempotente).
        playbackController.initialize()
        // Restaura el último orden elegido (persistido en DataStore).
        viewModelScope.launch {
            val savedSort = settingsRepository.sortOrder.first()
            _uiState.update { it.copy(sort = savedSort, songs = sortSongs(allSongs, savedSort)) }
        }
    }

    /** Reproduce la lista actual empezando por la canción tocada. */
    fun play(index: Int) {
        playbackController.playSongs(_uiState.value.songs, index)
    }

    fun togglePlayPause() {
        playbackController.togglePlayPause()
    }

    fun next() = playbackController.next()

    fun previous() = playbackController.previous()

    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)

    fun toggleShuffle() = playbackController.toggleShuffle()

    fun cycleRepeat() = playbackController.cycleRepeat()

    /** Se llama una vez que el permiso está concedido. */
    fun loadSongs() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                allSongs = repository.getSongs()
                customOrder = repository.getCustomOrder()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loaded = true,
                        songs = sortSongs(allSongs, it.sort),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error leyendo canciones")
                }
            }
        }
    }

    fun setSort(order: SortOrder) {
        _uiState.update { it.copy(sort = order, songs = sortSongs(allSongs, order)) }
        viewModelScope.launch { settingsRepository.setSortOrder(order) }
    }

    /**
     * Reordena una canción (solo tiene sentido en orden CUSTOM). Actualiza la lista
     * en memoria al instante (para que el arrastre se vea fluido) y persiste con debounce.
     */
    fun moveSong(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.songs
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val newList = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        _uiState.update { it.copy(songs = newList) }
        customOrder = newList.mapIndexed { index, song -> song.id to index }.toMap()
        persistOrder(newList.map { it.id })
    }

    private fun persistOrder(orderedIds: List<Long>) {
        // Debounce: se guarda 400ms después del último movimiento, no en cada swap.
        saveOrderJob?.cancel()
        saveOrderJob = viewModelScope.launch {
            delay(400)
            repository.saveCustomOrder(orderedIds)
        }
    }

    private fun sortSongs(songs: List<Song>, order: SortOrder): List<Song> = when (order) {
        SortOrder.NAME -> songs.sortedBy { it.title.lowercase() }
        SortOrder.DURATION -> songs.sortedBy { it.durationMs }
        SortOrder.DATE_MODIFIED -> songs.sortedByDescending { it.dateModifiedSec }
        // Con posición guardada primero (por posición); las nuevas, al final por nombre.
        SortOrder.CUSTOM -> songs.sortedWith(
            compareBy({ customOrder[it.id] ?: Int.MAX_VALUE }, { it.title.lowercase() }),
        )
    }
}
