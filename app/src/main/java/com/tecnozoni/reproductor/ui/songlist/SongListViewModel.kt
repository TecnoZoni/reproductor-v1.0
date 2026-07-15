package com.tecnozoni.reproductor.ui.songlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecnozoni.reproductor.data.SongRepository
import com.tecnozoni.reproductor.data.model.Song
import com.tecnozoni.reproductor.data.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongListUiState())
    val uiState: StateFlow<SongListUiState> = _uiState.asStateFlow()

    private var allSongs: List<Song> = emptyList()

    /** Se llama una vez que el permiso está concedido. */
    fun loadSongs() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                allSongs = repository.getSongs()
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
    }

    private fun sortSongs(songs: List<Song>, order: SortOrder): List<Song> = when (order) {
        SortOrder.NAME -> songs.sortedBy { it.title.lowercase() }
        SortOrder.DURATION -> songs.sortedBy { it.durationMs }
        SortOrder.DATE_MODIFIED -> songs.sortedByDescending { it.dateModifiedSec }
    }
}
