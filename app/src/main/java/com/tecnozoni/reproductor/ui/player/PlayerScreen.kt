package com.tecnozoni.reproductor.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tecnozoni.reproductor.playback.PlaybackState
import com.tecnozoni.reproductor.ui.songlist.components.PlayerArtwork
import com.tecnozoni.reproductor.ui.songlist.components.RepeatButton
import com.tecnozoni.reproductor.ui.songlist.components.ShuffleButton
import com.tecnozoni.reproductor.ui.songlist.components.formatDuration

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.playbackState.collectAsStateWithLifecycle()

    PlayerContent(
        state = state,
        onBack = onBack,
        onOpenQueue = onOpenQueue,
        onTogglePlayPause = viewModel::togglePlayPause,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onSeek = viewModel::seekTo,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeat,
        modifier = modifier,
    )
}

@Composable
private fun PlayerContent(
    state: PlaybackState,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragMs by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(0L)
    val shownMs = dragMs?.toLong() ?: state.positionMs

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding() // respeta status bar y barra de navegación (edge-to-edge)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            // Barra superior: volver + ver cola.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    Text("▾  Volver", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenQueue) {
                    Text("Cola", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Carátula grande.
            PlayerArtwork(
                uri = state.currentUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = state.currentTitle ?: "",
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = state.currentArtist ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )

            Spacer(Modifier.weight(1f))

            Slider(
                value = shownMs.toFloat(),
                onValueChange = { dragMs = it },
                onValueChangeFinished = {
                    dragMs?.let { onSeek(it.toLong()) }
                    dragMs = null
                },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(formatDuration(shownMs), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(16.dp))

            // Controles principales.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShuffleButton(enabled = state.shuffleEnabled, onClick = onToggleShuffle)
                TextButton(onClick = onPrevious) {
                    Text("⏮", style = MaterialTheme.typography.headlineMedium)
                }
                TextButton(onClick = onTogglePlayPause) {
                    Text(
                        text = if (state.isPlaying) "⏸" else "▶",
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
                TextButton(onClick = onNext) {
                    Text("⏭", style = MaterialTheme.typography.headlineMedium)
                }
                RepeatButton(repeatMode = state.repeatMode, onClick = onCycleRepeat)
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
