package com.tecnozoni.reproductor.ui.songlist.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.media3.common.Player

/** Botón de shuffle. Se pinta con el color primario cuando está activo. */
@Composable
fun ShuffleButton(enabled: Boolean, onClick: () -> Unit, style: TextStyle = MaterialTheme.typography.titleLarge) {
    TextButton(onClick = onClick) {
        Text(
            text = "🔀",
            style = style,
            color = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Botón de repeat: OFF (apagado) / ALL (🔁) / ONE (🔂). Coloreado cuando está activo. */
@Composable
fun RepeatButton(repeatMode: Int, onClick: () -> Unit, style: TextStyle = MaterialTheme.typography.titleLarge) {
    val active = repeatMode != Player.REPEAT_MODE_OFF
    val symbol = if (repeatMode == Player.REPEAT_MODE_ONE) "🔂" else "🔁"
    TextButton(onClick = onClick) {
        Text(
            text = symbol,
            style = style,
            color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
