package com.tecnozoni.reproductor.ui.songlist.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.tecnozoni.reproductor.R

/** Botón de aleatorio. Blanco (primary) cuando está activo, gris cuando no. */
@Composable
fun ShuffleButton(enabled: Boolean, onClick: () -> Unit, size: Dp = 24.dp) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_shuffle),
            contentDescription = "Aleatorio",
            tint = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size),
        )
    }
}

/** Botón de repetir: OFF (gris) / ALL (repeat) / ONE (repeat_one), blanco cuando activo. */
@Composable
fun RepeatButton(repeatMode: Int, onClick: () -> Unit, size: Dp = 24.dp) {
    val active = repeatMode != Player.REPEAT_MODE_OFF
    val icon = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = "Repetir",
            tint = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size),
        )
    }
}
