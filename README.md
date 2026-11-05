# Aprendo IA Jugando en Santiago

Aplicación Android para **Triskeledu SpA** que convierte el recorrido por Santiago Centro en un juego para aprender sobre inteligencia artificial. El usuario explora un mapa, se acerca a un lugar, escanea su código QR, ve una cápsula de video sobre IA, se toma una selfie y suma puntos que lo hacen subir de nivel y desbloquear temas.

Proyecto de la asignatura **DSY1105 Desarrollo de Aplicaciones Móviles** (Duoc UC), Evaluación Parcial 3. Es un MVP académico: no se publica en Google Play, trabaja con datos ficticios y **no tiene backend**: lo que dependería de un servidor se simula en el mismo teléfono (`data/simulacion/`).

## Integrantes

| Integrante | GitHub | Responsabilidades principales |
| --- | --- | --- |
| Simón Inostroza | `simoninos` | Mapa, detalle del punto (esfera, estado y comentarios), navegación, tema visual, ubicación GPS, estado de la red y modelo de puntos de interés |
| Gabriel Sandoval | `Sandy-Yera` | Sesión, registro y recuperación, configuración, niveles y temas, creador de contenido, administrador, notificaciones, sincronización simulada, contenedor de dependencias y Room |
| Catalina Lobos | `Shany-Ghost` | Escaneo de QR, lugar encontrado, Mis descubrimientos, progreso del usuario, comentarios, esfera de agua y pruebas unitarias |

## Puntajes y niveles

Cada lugar entrega hasta **80 puntos**: escanear el QR (+10), ver la cápsula completa (+50) y subir una selfie (+20).

| Nivel | Nombre | Desde | Tema que desbloquea |
| --- | --- | --- | --- |
| 1 | Visitante | 0 | Clásico |
| 2 | Explorador | 35 | Cerro Santa Lucía |
| 3 | Descubridor | 105 | Atardecer en el Mapocho |
| 4 | Guía urbano | 210 | Noche de museo |
| 5 | Cronista | 350 | Desierto florido |
| 6 | Guardián | 525 | Bosque nativo |
| 7 | Sabio | 735 | Glaciar |
| 8 | Maestro IA | 980 | IA neón |

Cada tema tiene versión Día y Noche. Si los puntos bajan (por ejemplo, cuando el servidor rechaza un escaneo), el nivel baja pero los temas ya desbloqueados se conservan, y "¡Subiste de nivel!" no se repite al recuperar un nivel que ya se tuvo.

## Funcionalidades

### Cliente
- **Bienvenida, registro e inicio de sesión** con validación bajo cada campo (contraseñas con hash PBKDF2 y sal). En el registro se puede pedir ser **creador de contenido**: la cuenta queda "En revisión" hasta que un administrador la apruebe. Los términos se leen desde "Leer los términos" y explican el uso de la ubicación, también en segundo plano.
- **Acceso rápido (demo)** en el inicio de sesión: rellena el correo y la contraseña de cada usuario de prueba.
- **Recuperar contraseña** con un código de 6 dígitos que llega como notificación local (vence a los 15 minutos y sirve una vez).
- **Mapa** (OpenStreetMap) con la ubicación del usuario, el círculo de 60 m y un marcador por estado (bloqueado, desbloqueado, completado). Un lugar sin escanear a 60 m o menos **late con un halo** para indicar que se puede escanear. La tarjeta del usuario muestra su nivel y abre **Mi nivel** (niveles, temas y cuánto falta).
- Sin permiso de ubicación o con el GPS apagado, el mapa y Escanear muestran **"Activa la ubicación para encontrar lugares"** con un botón que pide el permiso, abre los ajustes de la app (si se negó dos veces) o los del GPS.
- **Escanear QR**: desbloquea solo a 60 m o menos (+10 y la celebración "Lugar encontrado"). Un código ajeno muestra "Este código no es de Aprendo IA"; un lugar inactivo o eliminado, "Este lugar no está disponible"; uno ya escaneado, "Ya descubriste este lugar" y abre su detalle sin sumar.
- **Detalle del lugar**: esfera de avance, distancia, comentarios y las tres actividades:
  - **Cápsula de video** (Media3): la primera vez va bloqueada (no se puede adelantar) y al llegar al final suma 50; después queda libre. La Moneda usa un clip de prueba incluido en la app; las demás, mientras no tengan enlace, se marcan vistas al tocarlas.
  - **Selfie** con la cámara frontal (+20).
- **Mis descubrimientos**: la tarjeta "Mis puntos" con **Ver ranking ›**, "Lugares escaneados X de Y" y los cuadros QR, Cápsulas y Selfie. Cada vista lista lo desbloqueado y lo que falta por descubrir, que se puede abrir en el mapa.
- **Ranking** (P20): los usuarios del teléfono ordenados por puntos, **este mes** o **siempre**, con el usuario actual destacado aunque no esté entre los primeros. El administrador no aparece.
- **Notificaciones** (campana con contador): lugar cercano (a 200 m, una vez al día por lugar), nueva cápsula, logros (lugar completado y subida de nivel) y avisos de la cuenta. Todas quedan en el historial aunque el sistema no las muestre.
- **"Lugar cercano" con la app minimizada**: si se permite la ubicación "todo el tiempo", un servicio en primer plano (con su notificación fija "Buscando lugares cercanos") sigue avisando. Si no se permite, funciona solo con la app abierta y Configuración explica cómo activarlo.
- **Perfil**: cambiar nombre de usuario y contraseña.
- **Configuración**: tema y modo Día/Noche, interruptores de notificaciones, seguir la ubicación, mostrar bloqueados y cerrar sesión.

### Creador de contenido
- **Mis cápsulas**: crear, editar, probar el video (vista previa libre que no suma), publicar, retirar y eliminar, con sus reglas.
- Asociar una cápsula publicada a un punto **Sin cápsula** desde el mapa; el punto queda Inactivo y se avisa al administrador.

### Administrador
- **Mis lugares (P07-Admin)**: lista con filtros por estado, interruptor para activar o desactivar y eliminación (solo si nadie lo ha escaneado).
- **Crear y editar puntos** con foto, ubicación elegida en el mapa, cápsula asociada y QR generado al guardar (`TRISKEL-PI-NNN`), que se puede descargar.
- **Solicitudes de creador**: aprobar o rechazar desde la campana.
- En el mapa ve también los puntos Sin cápsula e Inactivos.

### Sincronización simulada (sin backend)
- Todo se guarda primero en el teléfono (Room) con `sincronizado = false`.
- Al volver la red, **WorkManager** envía lo pendiente de la cuenta conectada a `ServidorSimulado`, que revalida cada escaneo (60 m o menos y sin duplicado) y cada comentario. Lo aceptado pasa a `sincronizado = true`.
- Si un escaneo no pasa, se quitan los puntos de ese lugar, llega la notificación **"No pudimos validar tu visita a [lugar]"** y el resto se mantiene.
- `ServidorApi` es la interfaz que la app usa para hablar con el servidor: cuando exista el backend, una versión con Retrofit la implementa y solo cambia `AppContainer`.

## Usuarios de prueba

Contraseña de todos: **`Demo1234`**. También están en "Acceso rápido (demo)" del inicio de sesión.

| Usuario | Correo | Rol | Nivel | Puntos | Qué muestra |
| --- | --- | --- | --- | --- | --- |
| `usuaria_nueva` | usuaria.nueva@ejemplo.cl | Cliente | 1 · Visitante | 0 | Sin lugares desbloqueados |
| `explorador_demo` | explorador.demo@ejemplo.cl | Cliente | 2 · Explorador | 70 | Su visita a la Catedral se guardó sin conexión a 300 m: al entrar con internet, el servidor la rechaza, baja a 10 puntos y al nivel 1, y conserva el tema Cerro Santa Lucía |
| `camila_explora` | camila.explora@ejemplo.cl | Cliente | 3 · Descubridor | 160 | Un lugar en cada estado (recién descubierto, falta cápsula, falta selfie, completado) |
| `tomas_ruta` | tomas.ruta@ejemplo.cl | Cliente | 4 · Guía urbano | 220 | Tema Noche y dos lugares completados |
| `contenido_ia` | contenido@ejemplo.cl | Creador | 5 · Cronista | 400 | Mis cápsulas, una en borrador y el mapa del creador |
| `guardian_demo` | guardian.demo@ejemplo.cl | Cliente | 6 · Guardián | 560 | Siete lugares completados |
| `sabio_demo` | sabio.demo@ejemplo.cl | Cliente | 7 · Sabio | 800 | Diez lugares completados |
| `maestro_demo` | maestro.demo@ejemplo.cl | Cliente | 8 · Maestro IA | 1040 | Los 8 temas y el primer lugar del ranking |
| `admin_triskel` | admin@ejemplo.cl | Administrador | Sin nivel | — | Mis lugares del administrador, solicitudes y todos los temas |
| `pendiente_demo` | pendiente.demo@ejemplo.cl | Solicitud de creador | En revisión | — | La pantalla "Cuenta en revisión" al iniciar sesión |

La base se llena con estos datos **solo la primera vez**. Si la app ya estaba instalada, desinstálala o borra sus datos (*Ajustes → Apps → Aprendo IA → Almacenamiento → Borrar datos*) para verlos.

## Enlazar los videos (ENLACES_CAPSULAS)

Cuando existan los videos de las cápsulas:

1. En Google Drive, comparte cada video como **"Cualquier persona con el enlace"** y copia el enlace.
2. Abre `app/src/main/java/com/cookingplastic/triskeledu/data/local/DatosPrueba.kt` y busca el bloque `ENLACES_CAPSULAS`, al inicio del archivo.
3. Pega cada enlace entre comillas en lugar de `null`, en la línea del lugar que corresponde:
   ```kotlin
   val LA_MONEDA: String? = "https://drive.google.com/file/d/ID_DEL_VIDEO/view?usp=sharing"
   ```
4. Compila e instala de nuevo, y borra los datos de la app para que la base se cree con los enlaces.

Mientras un lugar quede en `null`, su cápsula funciona sin video (RN-35) y La Moneda usa el clip de prueba. El creador también puede pegar enlaces desde la app, al editar una cápsula.

## Cómo ejecutar

**Requisitos**
- Android Studio reciente, compatible con Android Gradle Plugin 9.3 (Gradle se descarga solo con el wrapper).
- JDK 17 o superior (el que trae Android Studio sirve).
- Emulador o teléfono con **Android 8.0 (API 26)** o superior y Google Play Services. La app compila contra la API 36.

**Pasos**
1. Clonar el repositorio (la rama principal es `main/components`):
   ```
   git clone https://github.com/simoninos/CodigoUrbano.git
   ```
2. Abrir la carpeta en Android Studio y esperar la sincronización de Gradle.
3. Elegir el emulador o el teléfono y presionar **Run ▶** (configuración `app`).
4. Aceptar los permisos de **ubicación**, **cámara** y **notificaciones** cuando la app los pida.
5. Pruebas unitarias: `./gradlew test` (en Windows, `gradlew test`).

**En el emulador**
- Fija la ubicación en Santiago: *Extended Controls (⋯) → Location*, por ejemplo en La Moneda (-33.44294, -70.65388). A menos de 60 m de un lugar sin escanear, su marcador late.
- Para probar la sincronización: activa el modo avión, escanea un lugar y desactiva el modo avión; WorkManager lo envía solo.
- Para probar el rechazo (EXC-08): entra con `explorador_demo` con internet.

## APK release firmado (RNF-08)

La llave de firma **nunca se sube al repositorio**: `*.jks`, `*.keystore` y `keystore.properties` están en `.gitignore`.

1. Crear la llave una sola vez (en Android Studio: *Build → Generate Signed App Bundle or APK → APK → Create new…*), o con:
   ```
   keytool -genkeypair -v -keystore aprendo-ia.jks -alias aprendo-ia -keyalg RSA -keysize 2048 -validity 10000
   ```
   Guarda el archivo `.jks` **fuera del proyecto** (o en la raíz, que ya está ignorada) y anota las contraseñas en un lugar seguro.
2. Crear en la raíz del proyecto el archivo `keystore.properties` (no se sube):
   ```
   storeFile=../ruta/a/aprendo-ia.jks
   storePassword=la_contraseña_del_almacén
   keyAlias=aprendo-ia
   keyPassword=la_contraseña_de_la_llave
   ```
   `storeFile` es relativo a la raíz del proyecto (también sirve una ruta absoluta).
3. Generar el APK: `./gradlew :app:assembleRelease` (en Windows, `gradlew :app:assembleRelease`).
4. El APK queda en `app/build/outputs/apk/release/app-release.apk`. Se instala con `adb install app-release.apk` o copiándolo al teléfono.

Sin `keystore.properties`, `assembleRelease` genera un APK sin firmar (`app-release-unsigned.apk`), que Android no deja instalar.

## Arquitectura

La app usa **MVVM** con Jetpack Compose:

```
Pantalla (Composable)  ──eventos──▶  ViewModel  ──▶  Repository  ──▶  Room / GPS / Simulación
        ▲                                │
        └──────── StateFlow<UiState> ────┘
```

- **Model** (`data/model`): entidades (`Usuario`, `Preferencias`, `PuntoInteres`, `Capsula`, `ProgresoPunto`, `Comentario`, `Notificacion`, `CodigoRecuperacion`).
- **Repository** (`data/repository`, `data/location`, `data/red`): una interfaz por fuente de datos, con la versión Room o real y una versión `Fake…Repository` en memoria para las pruebas.
- **Reglas del juego** (`data/nivel`, `data/creador`, `data/notificaciones`, `data/sincronizacion`): nivel, cápsulas del creador, notificaciones locales y sincronización. Envuelven a los repositorios (`ProgresoConNivel`, `PuntosConAvisos`, `ProgresoConSincronizacion`) para que ninguna pantalla tenga que acordarse de ellas.
- **Simulación** (`data/simulacion`): recuperación de contraseña, solicitudes de creador y servidor de sincronización, en el mismo teléfono.
- **ViewModel** (`ui/viewmodels`): un `UiState` por pantalla y sus `UiEvent`. Las validaciones están en `ui/validation` y los errores se muestran bajo cada campo con `MensajeError`.
- **Screens y Composables** (`ui/screens`, `ui/components`): solo dibujan el estado. Los colores salen del tema (`ui/theme`), nunca escritos en una pantalla.

### Estructura de carpetas

```
app/src/main/java/com/cookingplastic/triskeledu/
├── MainActivity.kt, TriskeleduApp.kt, AppContainer.kt
├── data/
│   ├── model/           Entidades
│   ├── local/           AppDatabase (Room v3), DAO, carga inicial y DatosPrueba (con ENLACES_CAPSULAS)
│   ├── repository/      Repositorios (interfaz + Room + Fake)
│   ├── location/        GPS y servicio de "Lugar cercano" en segundo plano
│   ├── nivel/           Nivel, temas y "¡Subiste de nivel!"
│   ├── creador/         Reglas de las cápsulas del creador
│   ├── notificaciones/  Notificaciones locales y sus reglas
│   ├── sincronizacion/  ServidorApi, Sincronizador y WorkManager
│   ├── simulacion/      Servidor simulado, recuperación y solicitudes
│   ├── red/             Estado de la conexión
│   └── security/        Hash de contraseñas
├── navigation/          Rutas, barra inferior por rol y NavHost
└── ui/
    ├── screens/         Una carpeta por pantalla
    ├── components/      Composables reutilizables (mapa, esfera, avisos, botones, diálogos…)
    ├── state/, event/   UiState y UiEvent de cada pantalla
    ├── viewmodels/      ViewModels
    ├── validation/      Reglas de formularios, escaneo, mapa, QR, cápsulas y nivel
    └── theme/           8 temas con Día y Noche (Material 3)
```

### Persistencia local
- **Room (SQLite)**, versión 3, guarda usuarios, sesión, preferencias, puntos, cápsulas, progreso, comentarios, notificaciones y códigos de recuperación.
- La primera vez que se abre la app, `CargaInicial` llena la base con `DatosPrueba`.
- `AppContainer` crea la base y los repositorios una sola vez; cada ViewModel los recibe con su fábrica.

## Tecnologías

| Área | Herramienta |
| --- | --- |
| Lenguaje | Kotlin 2.0 |
| Interfaz | Jetpack Compose, Material 3, Navigation Compose |
| Estado | ViewModel, StateFlow, corrutinas |
| Persistencia | Room 2.8 |
| Trabajo en segundo plano | WorkManager, servicio en primer plano |
| Mapa | osmdroid 6.1 (OpenStreetMap) |
| Ubicación | Google Play Services Location |
| Cámara y QR | CameraX, ML Kit (lectura), ZXing (generación) |
| Video | Media3 ExoPlayer |
| Imágenes | Coil 3 |
| Pruebas | JUnit 4, kotlinx-coroutines-test |

## Pendiente (requiere backend)

- Backend en Spring Boot (BKD-01 a BKD-07): hoy lo reemplaza `data/simulacion/`.
- Recuperación de contraseña por correo real, sesión que vence en el servidor (EXC-07) y servidor caído (EXC-06).
- Videos reales en Drive (V1 a V6): se enlazan en `ENLACES_CAPSULAS`.

## Flujo de ramas

- `main/components`: rama principal e integración del equipo; cada integrante abre un pull request desde su rama.
- `simon`, `Gabriel`, `Catalina`: ramas de trabajo de cada integrante.
