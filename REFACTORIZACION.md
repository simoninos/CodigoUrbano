# Refactorización por funcionalidad: lo hecho y lo pendiente

Rama `simon-refactor-paquetes`, que sale de `b8263c8` (tag `antes-refactor-paquetes`). 11 de noviembre de 2026.

`ui/` dejó de estar ordenado por tipo de archivo y quedó ordenado por pantalla. Las reglas pasaron a `domain/rules`. Las interfaces de repositorio que tenían archivo propio pasaron a `domain/repository`. Solo cambiaron paquetes, imports y la ubicación de los archivos: no se dividió ningún archivo, no se escribió código nuevo y no se tocó Room.

## Verificación

| Prueba | Antes | Después |
| --- | --- | --- |
| `assembleDebug` | Compila | Compila, con los mismos avisos de APIs obsoletas (`Icons.Filled.ArrowBack`, `Locale(String, String)`) |
| `testDebugUnitTest` | 28 clases, 167 pruebas, 0 fallas | 28 clases, 167 pruebas, 0 fallas |
| `lintDebug` | No se midió | Falla antes de revisar: `androidx.compose.ui:ui-test-junit4` no tiene versión (ver pendientes) |
| Recorrido manual en el teléfono | — | Pendiente |

Además del build, se revisó con un script que, en cada uno de los 245 archivos Kotlin, todo nombre del proyecto que se resolvía antes se siga resolviendo después.

## Qué se movió

115 archivos movidos; en 170 se ajustaron el `package` o los imports.

| De | A |
| --- | --- |
| `ui/viewmodels`, `ui/state`, `ui/event` (69 archivos) | `ui/screens/<pantalla>/` junto a su pantalla, salvo los de la fila siguiente |
| `SesionViewModel`, `SesionUiState`, `SesionUiEvent`, `RolViewModel`, `TemaViewModel`, `SubidaNivelViewModel`, `SubidaNivelUiEvent` | `ui/shared` |
| `AdminListaViewModel`, `AdminListaUiState`, `AdminListaUiEvent` y `screens/mislugares/MisLugaresAdminScreen.kt` | `ui/screens/admin` |
| `screens/creador/ComponentesCreador.kt` (`Etiqueta`, `CabeceraCreador`…) | `ui/components` |
| `ui/validation/*` (11 reglas, Kotlin puro) | `domain/rules` |
| `data/repository/{Capsula, Comentario, Preferencias, Progreso, PuntoInteres, Usuario}Repository.kt` (interfaces) | `domain/repository` |
| 27 pruebas de `src/test` | El paquete de lo que prueba (`ui/screens/...`, `ui/shared`, `domain/rules`, `data/model`, `data/notificaciones`, `data/sincronizacion`, `ui/theme`) |

También:

- Se creó `data/remote/README.md`, que explica dónde va la API.
- Se actualizó la sección de estructura del `README.md`.
- Se borraron las carpetas vacías `ui/viewmodels`, `ui/state`, `ui/event` y `ui/validation`.
- `ExampleUnitTest` y `ExampleInstrumentedTest` se quedan en el paquete raíz.

## Criterios del plan

| Criterio | Estado |
| --- | --- |
| Compila y pasan las mismas pruebas | Cumplido: 167 de 167 |
| No quedan `ui/viewmodels`, `ui/state`, `ui/event` ni `ui/validation` | Cumplido |
| Ninguna carpeta de `ui/screens/` importa de otra | Faltan 2 excepciones (pendientes 1 y 2) |
| `domain/` no importa `android.*` ni `androidx.*` | Cumplido |
| Los ViewModels reciben interfaces de `domain/repository` | Parcial (pendientes 3 y 4) |
| Room no cambió | Cumplido: no se tocaron entidades, DAOs, versión ni migraciones |
| Recorrido manual sin diferencias | Pendiente |

## Pendientes

1. **`creador/` importa `video/`.** `CapsulaPreviaScreen` reutiliza `VideoScreen`, `VideoViewModel`, `VideoUiState` y `textoTiempo`. Se dejó como excepción documentada, como se decidió.
2. **`admin/` importa `creador/`.** `MisLugaresAdminScreen` usa `EtiquetaEstadoPunto`, que está declarada dentro de `creador/CapsulaDetalleScreen.kt`. Para cumplir el criterio hay que pasar esa función a `ui/components`, y eso implica partir un archivo.
3. **Archivos con la interfaz y la implementación juntas**, que siguen en `data/`:
   - `NotificacionRepository.kt` (con `RoomNotificacionRepository` y su versión `Fake`);
   - `red/RedRepository.kt` (con `AndroidRedRepository` y `FakeRedRepository`);
   - `location/UbicacionRepository.kt` (con `GpsUbicacionRepository` y `FakeUbicacionRepository`);
   - `simulacion/RecuperacionRepository.kt` y `simulacion/SolicitudesRepository.kt` (con sus versiones simuladas).

   Hay que separar cada uno y llevar la interfaz a `domain/repository`.
4. **`GestorCapsulas` no tiene interfaz.** Es una clase concreta de `data/creador` y la reciben 4 ViewModels del creador (`AsociarCapsula`, `CapsulaDetalle`, `CapsulaEditar`, `CapsulaPrevia`).
5. **Las entidades Room siguen en `data/model`,** como se decidió. Por eso `domain/rules` y `domain/repository` importan clases de `data` (`Capsula`, `PuntoInteres`, `Usuario`…). Se resuelve cuando llegue la API, con modelos de dominio y *mappers*. Hasta entonces no se puede separar `:domain` como módulo de Gradle (etapa 7).
6. **Reglas puras que siguen en `data`:** `nivel/ControlNivel`, `nivel/ProgresoConNivel`, `notificaciones/ReglaAvisos` y `simulacion/ReglaSincronizacion`. Son candidatas a `domain/rules`, pero no estaban en el plan.
7. **`lintDebug` no corre,** porque `androidx.compose.ui:ui-test-junit4` se declara para `androidTest` sin versión (probablemente falta `androidTestImplementation(platform(libs.androidx.compose.bom))`). No viene de la refactorización: no se tocó ningún archivo de Gradle.
8. **Recorrido manual (etapa 8.2):** instalar el APK encima de la versión anterior y recorrer todas las pantallas con los usuarios de prueba del README. También hay que confirmar que los datos guardados siguen ahí.
9. **Etapa 7 (módulos de Gradle) y Hilt:** fuera de este trabajo.

## Notas para el equipo

- Cualquier rama que toque `ui/viewmodels`, `ui/state`, `ui/event` o `ui/validation` va a tener conflictos al integrar. La tabla de arriba dice dónde quedó cada archivo.
- Git debería reconocer los movimientos como renombres (`git add -A` y después `git status` muestra `renamed:`), así que se conserva el historial de cada archivo.
