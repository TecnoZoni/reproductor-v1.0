package com.tecnozoni.reproductor.ui.songlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecnozoni.reproductor.data.SettingsRepository
import com.tecnozoni.reproductor.data.SongRepository
import com.tecnozoni.reproductor.data.model.Song
import com.tecnozoni.reproductor.data.model.SortDirection
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
    val direction: SortDirection = SortDirection.ASC,
    val query: String = "",
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
    // Lista completa ya ordenada (sin filtrar). El buscador filtra sobre esto.
    private var sortedSongs: List<Song> = emptyList()
    // Orden personalizado guardado: id -> posición.
    private var customOrder: Map<Long, Int> = emptyMap()
    private var saveOrderJob: Job? = null

    init {
        // Conecta la UI con el servicio de reproducción (idempotente).
        playbackController.initialize()
        // Restaura el último orden + dirección elegidos (persistido en DataStore).
        viewModelScope.launch {
            val pref = settingsRepository.sortPreference.first()
            _uiState.update { it.copy(sort = pref.order, direction = pref.direction) }
            rebuildSorted()
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
                _uiState.update { it.copy(isLoading = false, loaded = true) }
                rebuildSorted()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error leyendo canciones")
                }
            }
        }
    }

    /**
     * Selecciona un orden. Si se re-toca el mismo (y no es CUSTOM), invierte la dirección.
     * Al elegir uno distinto, arranca en ascendente.
     */
    fun setSort(order: SortOrder) {
        val state = _uiState.value
        val newDirection = when {
            order == SortOrder.CUSTOM -> SortDirection.ASC
            order == state.sort ->
                if (state.direction == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
            else -> SortDirection.ASC
        }
        _uiState.update { it.copy(sort = order, direction = newDirection) }
        rebuildSorted()
        viewModelScope.launch { settingsRepository.setSort(order, newDirection) }
    }

    /** Filtra la lista por título o artista (en memoria, instantáneo). */
    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        applyFilter()
    }

    private fun rebuildSorted() {
        sortedSongs = sortSongs(allSongs, _uiState.value.sort, _uiState.value.direction)
        applyFilter()
    }

    private fun applyFilter() {
        val q = _uiState.value.query.trim()
        val filtered = if (q.isBlank()) {
            sortedSongs
        } else {
            sortedSongs.filter { it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) }
        }
        _uiState.update { it.copy(songs = filtered) }
    }

    /**
     * Reordena una canción (solo tiene sentido en orden CUSTOM). Actualiza la lista
     * en memoria al instante (para que el arrastre se vea fluido) y persiste con debounce.
     */
    fun moveSong(fromIndex: Int, toIndex: Int) {
        // No se reordena mientras hay una búsqueda activa (la lista está filtrada).
        if (_uiState.value.query.isNotBlank()) return
        val current = _uiState.value.songs
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val newList = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        sortedSongs = newList
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

    private fun sortSongs(songs: List<Song>, order: SortOrder, direction: SortDirection): List<Song> {
        // CUSTOM es manual: no se invierte.
        if (order == SortOrder.CUSTOM) {
            return songs.sortedWith(
                compareBy({ customOrder[it.id] ?: Int.MAX_VALUE }, { it.title.lowercase() }),
            )
        }
        val ascending = when (order) {
            SortOrder.NAME -> songs.sortedBy { it.title.lowercase() }
            SortOrder.DURATION -> songs.sortedBy { it.durationMs }
            SortOrder.DATE_MODIFIED -> songs.sortedBy { it.dateModifiedSec }
            SortOrder.CUSTOM -> songs // inalcanzable
        }
        return if (direction == SortDirection.ASC) ascending else ascending.reversed()
    }
}
