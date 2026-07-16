package com.tecnozoni.reproductor.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tecnozoni.reproductor.ui.player.PlayerScreen
import com.tecnozoni.reproductor.ui.songlist.SongListScreen

/** Rutas de la app. */
private object Routes {
    const val LIST = "list"
    const val PLAYER = "player"
}

/**
 * Grafo de navegación: lista de canciones <-> pantalla de reproducción completa.
 * El NavController mantiene el back stack (el botón atrás vuelve a la lista).
 */
@Composable
fun ReproductorNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            SongListScreen(onOpenPlayer = { navController.navigate(Routes.PLAYER) })
        }
        composable(Routes.PLAYER) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
    }
}
