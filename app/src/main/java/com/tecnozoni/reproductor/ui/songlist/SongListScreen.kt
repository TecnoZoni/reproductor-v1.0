package com.tecnozoni.reproductor.ui.songlist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tecnozoni.reproductor.data.model.Song
import com.tecnozoni.reproductor.data.model.SortOrder
import com.tecnozoni.reproductor.playback.PlaybackState
import com.tecnozoni.reproductor.ui.songlist.components.SongRow

/**
 * Pantalla única. Primero resuelve el permiso de lectura de audio (que cambia
 * según la versión de Android), después muestra la lista.
 */
@Composable
fun SongListScreen(
    modifier: Modifier = Modifier,
    viewModel: SongListViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    // Permiso correcto según versión: API 33+ = READ_MEDIA_AUDIO; 29-32 = READ_EXTERNAL_STORAGE.
    val audioPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    // Cuando el permiso está (o pasa a estar) concedido, cargamos la lista.
    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.loadSongs()
    }

    // Cada vez que la app vuelve al frente re-consultamos MediaStore, así aparecen
    // las canciones nuevas sin tener que cerrar y abrir la app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = ContextCompat.checkSelfPermission(context, audioPermission) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            hasPermission = true
            viewModel.loadSongs()
        }
    }

    if (!hasPermission) {
        PermissionGate(
            onRequest = { permissionLauncher.launch(audioPermission) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    },
                )
            },
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    SongListContent(
        uiState = uiState,
        playbackState = playbackState,
        onSortSelected = viewModel::setSort,
        onRefresh = viewModel::loadSongs,
        onSongClick = viewModel::play,
        onTogglePlayPause = viewModel::togglePlayPause,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongListContent(
    uiState: SongListUiState,
    playbackState: PlaybackState,
    onSortSelected: (SortOrder) -> Unit,
    onRefresh: () -> Unit,
    onSongClick: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Reproductor") },
                actions = {
                    TextButton(onClick = onRefresh) { Text("Actualizar") }
                    SortMenu(current = uiState.sort, onSortSelected = onSortSelected)
                },
            )
        },
        bottomBar = {
            if (playbackState.hasCurrent) {
                MiniPlayer(state = playbackState, onTogglePlayPause = onTogglePlayPause)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                // Spinner a pantalla completa solo en la carga INICIAL (lista vacía).
                uiState.isLoading && uiState.songs.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                uiState.error != null && uiState.songs.isEmpty() -> CenteredMessage(
                    text = "No se pudieron leer las canciones:\n${uiState.error}",
                )

                uiState.loaded && uiState.songs.isEmpty() -> CenteredMessage(
                    text = "No se encontraron canciones en el dispositivo.",
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    // Barra fina de progreso durante un refresh (la lista sigue visible).
                    if (uiState.isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    SongList(
                        songs = uiState.songs,
                        onSongClick = onSongClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    onSongClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(items = songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                onClick = { onSongClick(index) },
            )
            HorizontalDivider()
        }
    }
}

/** Barra inferior con la canción actual y el botón play/pausa. */
@Composable
private fun MiniPlayer(
    state: PlaybackState,
    onTogglePlayPause: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.currentTitle ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                Text(
                    text = state.currentArtist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            TextButton(onClick = onTogglePlayPause) {
                // ⏸ pausa / ▶ reproducir (símbolos Unicode, sin librería de íconos).
                Text(
                    text = if (state.isPlaying) "⏸" else "▶",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortMenu(
    current: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) {
        Text("Ordenar")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        SortOption("Nombre", SortOrder.NAME, current) { onSortSelected(it); expanded = false }
        SortOption("Duración", SortOrder.DURATION, current) { onSortSelected(it); expanded = false }
        SortOption("Fecha de modificación", SortOrder.DATE_MODIFIED, current) {
            onSortSelected(it); expanded = false
        }
    }
}

@Composable
private fun SortOption(
    label: String,
    order: SortOrder,
    current: SortOrder,
    onSelected: (SortOrder) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(if (order == current) "✓  $label" else label) },
        onClick = { onSelected(order) },
    )
}

@Composable
private fun PermissionGate(
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Para mostrar tu música, la app necesita permiso para leer el audio del dispositivo.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRequest, modifier = Modifier.padding(top = 16.dp)) {
            Text("Conceder permiso")
        }
        Text(
            text = "Si ya lo denegaste, activalo desde Ajustes:",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 24.dp),
        )
        Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 8.dp)) {
            Text("Abrir Ajustes")
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(24.dp),
        )
    }
}
