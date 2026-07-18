# 🎵 Reproductor

Reproductor de música para Android, **nativo, minimalista y sin anuncios**. Una sola
pantalla con **toda** tu música, ordenable de varias formas (incluido un orden
personalizado arrastrable que se guarda), con reproducción sólida en segundo plano.

Hecho en **Kotlin + Jetpack Compose**. Uso personal, distribuido por fuera de Play Store.

---

## ✨ Características

- 📂 Lista con **todas las canciones** del dispositivo (leídas de `MediaStore`).
- 🔀 **Ordenar** por nombre, duración o fecha de modificación — con **dirección invertible**
  (tocás de nuevo y se da vuelta, con una flecha que indica ascendente/descendente).
- ✋ **Orden personalizado**: arrastrás las canciones a mano y ese orden **queda guardado**
  entre sesiones (Room).
- 🔎 **Buscador** instantáneo por título o artista.
- ▶️ **Reproducción completa**: play/pausa, anterior/siguiente, barra de progreso (seek),
  cola, aleatorio (shuffle) y repetición (una / toda la cola).
- 🖼️ **Carátulas** embebidas (con placeholder cuando no hay).
- 🧠 **Recuperación de metadata rota**: títulos con caracteres corruptos (mojibake) o
  “estéticos” de redes se limpian/recuperan lo mejor posible.
- 🔔 **Segundo plano real**: sigue sonando con la pantalla bloqueada, con **controles en la
  notificación, pantalla de bloqueo y auriculares/bluetooth**.
- 📞 **Foco de audio**: pausa solo cuando entra una llamada y reanuda al colgar.
- 🖤 **Diseño negro/blanco** moderno, monocromo (OLED-friendly), con íconos vectoriales propios.

---

## 🧱 Stack técnico

| Área | Tecnología |
|------|------------|
| Lenguaje | Kotlin `2.2.10` |
| UI | Jetpack Compose (BOM `2026.02.01`), Material 3 |
| Build | Gradle (Kotlin DSL) + AGP `9.3.0` |
| SDK | `minSdk 29` · `targetSdk 34` · `compileSdk 37` |
| Reproducción | Media3 / ExoPlayer + MediaSessionService `1.10.1` |
| Persistencia (orden custom) | Room `2.8.4` |
| Preferencias | DataStore Preferences `1.2.1` |
| Inyección de dependencias | Hilt `2.60.1` (KSP `2.2.10-2.0.2`) |
| Navegación | Navigation Compose `2.9.8` |
| Drag & drop | `sh.calvin.reorderable` `3.1.0` |
| Lectura de tags ID3 | jaudiotagger (fork Adonai) `2.3.15` (vía JitPack) |

---

## 🏗️ Arquitectura

**MVVM simple**, sin capas de más. El flujo va en una sola dirección:

```
UI (Compose)  ->  ViewModel  ->  Repository  ->  ┌ MediaStore (qué canciones existen)
                                                 └ Room       (orden personalizado)
```

- La UI observa estado inmutable (`StateFlow`) y solo dibuja; nunca toca datos directo.
- **Reproducción desacoplada de la pantalla:** un `PlaybackService` (MediaSessionService)
  tiene el ExoPlayer real y corre por su cuenta; la UI lo controla con un `MediaController`.
  Por eso la música sobrevive a cerrar la pantalla o cambiar de app.

### Estructura de carpetas

```
com.tecnozoni.reproductor/
├── ReproductorApp.kt          @HiltAndroidApp
├── MainActivity.kt            @AndroidEntryPoint (host de navegación)
├── di/AppModule.kt            provee Room (DB + DAO)
├── data/
│   ├── model/                 Song, SortOrder, SortDirection
│   ├── mediastore/            MediaStoreSource (query de canciones)
│   ├── local/                 Room: AppDatabase, CustomOrderEntity, CustomOrderDao
│   ├── tags/TagReader.kt      lectura de tags ID3 crudos (recuperación de metadata)
│   ├── SongRepository.kt      combina MediaStore + Room
│   └── SettingsRepository.kt  preferencias (DataStore)
├── playback/
│   ├── PlaybackService.kt     MediaSessionService + ExoPlayer
│   └── PlaybackController.kt  MediaController + estado de reproducción
└── ui/
    ├── ReproductorNavHost.kt  rutas: lista <-> player <-> cola
    ├── songlist/              pantalla principal (lista, orden, búsqueda, mini-player)
    ├── player/                pantalla de reproducción completa + cola
    └── theme/                 tema negro/blanco
```

---

## 🚀 Compilar y correr

### Requisitos
- **Android Studio** reciente (con AGP 9 / Kotlin 2.2).
- **JDK 21** (Gradle lo resuelve solo por toolchain si no lo tenés).
- Un dispositivo o emulador con **Android 10 (API 29) o superior**.

### Pasos
```bash
git clone https://github.com/TecnoZoni/Reproductor.git
cd Reproductor
```
1. Abrí el proyecto en Android Studio y hacé **Sync** (baja las dependencias; la primera vez tarda).
2. Conectá un dispositivo (o iniciá un emulador) y apretá **Run ▶️**.
3. Al abrir, la app pide **permiso para leer el audio**. Concedelo y vas a ver tu música.

O por línea de comandos:
```bash
./gradlew assembleDebug        # genera app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔧 Cómo aplicar mejoras (guía de desarrollo y testeo)

Flujo recomendado para hacer un cambio y **verificar que no rompiste nada**:

### 1. Rama por cambio
```bash
git checkout main && git pull
git checkout -b mi-mejora
```

### 2. Hacé el cambio y compilá
```bash
# Compilación rápida de Kotlin (feedback veloz):
./gradlew :app:compileDebugKotlin

# IMPORTANTE — validación completa (incluye checks que compileDebugKotlin NO corre,
# como la compatibilidad de dependencias). Corré esto antes de dar algo por bueno:
./gradlew :app:assembleDebug
```
> Nota: `compileDebugKotlin` puede pasar y aun así fallar `assembleDebug`. **Validá siempre con `assembleDebug`.**

### 3. Tests
```bash
./gradlew :app:testDebugUnitTest          # tests unitarios (JVM)
```

### 4. Probá en el dispositivo (checklist manual)
Instalá y verificá lo que tocaste. Checklist completo de humo:

- [ ] La app pide permiso y **lista** toda la música.
- [ ] **Ordenar** por nombre / duración / fecha; tocar de nuevo **invierte** (flecha ↑/↓).
- [ ] **Buscar** filtra al instante; la ✕ limpia y vuelve al tope.
- [ ] **Orden personalizado**: arrastrar reordena; cerrar y reabrir **mantiene el orden**.
- [ ] Tocar una canción **suena**; el mini-player aparece.
- [ ] **Bloquear pantalla** → sigue sonando; **notificación** con play/anterior/siguiente.
- [ ] Controles desde **bluetooth/auriculares**.
- [ ] **Player completo**: carátula, seek arrastrable, shuffle, repeat (3 modos), cola.
- [ ] Entra una **llamada** → pausa; al colgar → reanuda.

### 5. Verificá y subí
```bash
git add -A
git status            # ⚠️ MIRÁ esto SIEMPRE antes de commitear (nunca subas secretos)
git commit -m "feat: descripción del cambio"
git push -u origin mi-mejora
```
Cuando esté probado, mergeás a `main` (idealmente vía Pull Request) y, si es una versión,
podés etiquetarla (`git tag -a vX.Y -m "..."`).

---

## 🔐 Generar un APK firmado (release)

Para distribuir el APK necesitás **firmarlo con tu propio keystore**. El mismo keystore en
cada versión = las actualizaciones se reconocen como la misma app.

1. **Generá un keystore** (Android Studio: *Build → Generate Signed App Bundle / APK → Create new…*,
   o con `keytool -genkeypair -v -keystore reproductor.jks -alias reproductor -keyalg RSA -keysize 2048 -validity 10000`).
   Guardá el archivo `.jks` y sus contraseñas **en un lugar seguro y para siempre** (si los
   perdés, no vas a poder actualizar la app).
2. Copiá `keystore.properties.template` como **`keystore.properties`** en la raíz y completalo
   con tus datos:
   ```properties
   storeFile=C:/ruta/a/reproductor.jks
   storePassword=****
   keyAlias=reproductor
   keyPassword=****
   ```
   > ⚠️ `keystore.properties` y `*.jks` están en `.gitignore`. **Nunca los subas al repo.**
3. Generá el APK firmado:
   ```bash
   ./gradlew assembleRelease
   # -> app/build/outputs/apk/release/app-release.apk
   ```

---

## 📋 Permisos y notas de compatibilidad

- **Lectura de audio:** `READ_MEDIA_AUDIO` (API 33+) o `READ_EXTERNAL_STORAGE` (API 29–32).
- **Segundo plano:** foreground service de tipo `mediaPlayback` + `POST_NOTIFICATIONS` (API 33+).
- **Samsung / One UI:** la gestión agresiva de batería puede cortar el servicio en segundo
  plano. Si te pasa: *Ajustes → Batería → Reproductor → Sin restricciones* (o sacala de
  “apps en reposo”).

---

## 📄 Licencia

Distribuido bajo licencia **MIT**. Ver [LICENSE](LICENSE).

---

## ⬇️ Descargar la app (v1.0)

¿Solo querés usarla? Descargá el APK firmado listo para instalar:

**👉 [Descargar Reproductor v1.0 (APK)](https://drive.google.com/file/d/1HNWDjGZhWmEVfWNSIZOxVs67Am8gVYDD/view?usp=sharing)**

> Al instalarlo, Android te va a pedir permitir **“instalar apps de fuentes desconocidas”**
> para el navegador o explorador de archivos. Es normal en apps fuera de Play Store.
