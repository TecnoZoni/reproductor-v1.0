package com.tecnozoni.reproductor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application de la app. Punto de entrada de Hilt.
 *
 * @HiltAndroidApp genera el contenedor de dependencias raíz (a nivel de toda la
 * app) y engancha la inyección para Activities, ViewModels y Services.
 * Es el equivalente conceptual al contexto de Spring: acá "vive" el grafo de
 * objetos que Hilt sabe construir e inyectar.
 */
@HiltAndroidApp
class ReproductorApp : Application()
