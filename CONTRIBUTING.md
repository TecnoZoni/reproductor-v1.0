# Contribuir a Reproductor

¡Gracias por el interés! Este es un proyecto personal y open source (MIT). Las mejoras,
correcciones e ideas son bienvenidas. Esta guía explica cómo colaborar de forma ordenada.

> Antes que nada, leé el [README](README.md): tiene la **arquitectura**, la **estructura de
> carpetas** y cómo **compilar y correr** el proyecto.

---

## 🧰 Requisitos

- **Android Studio** reciente (con soporte para AGP 9 / Kotlin 2.2).
- **JDK 21** (Gradle lo resuelve por toolchain si no lo tenés instalado).
- Un dispositivo o emulador con **Android 10 (API 29) o superior** para probar de verdad.

---

## 🔁 Flujo de trabajo

1. **Fork** del repo (o cloná si tenés acceso de escritura).
2. Creá una **rama por cambio**, con nombre descriptivo:
   ```bash
   git checkout main && git pull
   git checkout -b feat/mi-mejora        # o fix/lo-que-arregla, docs/..., etc.
   ```
3. Hacé el cambio respetando las [convenciones](#-convenciones-de-código).
4. **Compilá y validá** (ver más abajo).
5. **Probá en un dispositivo** (checklist más abajo).
6. Commiteá siguiendo [Conventional Commits](#-commits).
7. Subí la rama y abrí un **Pull Request** hacia `main`, describiendo *qué* cambia y *por qué*.

---

## 🎨 Convenciones de código

- **Estilo Kotlin oficial** (`kotlin.code.style=official`). Android Studio ya lo aplica.
- **Arquitectura MVVM — respetá las capas.** El flujo es en una sola dirección:
  `UI (Compose) → ViewModel → Repository → (MediaStore + Room)`.
  - La UI **nunca** accede a datos directo: todo pasa por el `ViewModel` y el `Repository`.
  - El estado es **inmutable**: `data class` de UiState expuesto como `StateFlow`. El ViewModel
    emite un estado nuevo; Compose redibuja.
- **Composables** chicos, reutilizables y sin lógica de negocio (solo reciben estado y callbacks).
- **Íconos**: usá **vector drawables propios** en `res/drawable` (como los existentes). Evitá
  `material-icons-extended` — suma peso al APK y una de las prioridades del proyecto es la velocidad.
- **Comentarios en español**, como el resto del código. Comentá el *por qué*, no el *qué* obvio.
- Nada de dependencias nuevas “porque sí”: si sumás una, justificá el motivo en el PR.

---

## ✅ Validación antes de un PR (obligatorio)

```bash
# Validación COMPLETA (incluye checks que compileDebugKotlin NO corre, como la
# compatibilidad de dependencias). Es la que vale:
./gradlew :app:assembleDebug

# Tests unitarios:
./gradlew :app:testDebugUnitTest
```
> ⚠️ `compileDebugKotlin` puede pasar y aun así fallar `assembleDebug`. **Validá siempre con `assembleDebug`.**

**Probá en el dispositivo** lo que tocaste. Checklist de humo mínimo:

- [ ] La lista carga y se puede **ordenar** (nombre/duración/fecha, con dirección invertible).
- [ ] El **buscador** filtra y limpia bien.
- [ ] El **orden personalizado** se arrastra y **persiste** al reabrir la app.
- [ ] La **reproducción** anda: play/pausa, anterior/siguiente, seek, cola, shuffle, repeat.
- [ ] Suena en **segundo plano** con la **notificación** y controles de bluetooth.

---

## 🔒 Nunca subas secretos

- `keystore.properties`, `*.jks` y `*.keystore` están en `.gitignore`. **No los versiones.**
- **Mirá `git status` antes de cada commit.** Evitá `git add -A` a ciegas.
- La firma del APK se documenta en el [README](README.md#-generar-un-apk-firmado-release);
  cada quien usa su propio keystore.

---

## 🐛 Reportar bugs o pedir features

Abrí un **Issue** con:
- Qué esperabas que pasara y qué pasó realmente.
- **Pasos para reproducirlo.**
- Modelo del dispositivo y versión de Android.
- Si es un crash, el log de `Logcat` ayuda muchísimo.

---

## 📄 Licencia

Al contribuir, aceptás que tu aporte se distribuye bajo la licencia **MIT** del proyecto.
