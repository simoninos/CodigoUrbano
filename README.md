# Aprendo IA Jugando en Santiago

Aplicación Android para **Triskeledu SpA** que convierte el recorrido por Santiago Centro en un juego para aprender sobre inteligencia artificial. El usuario explora un mapa, se acerca a un punto de interés, escanea el código QR del lugar para desbloquearlo y suma puntos que se ven en una esfera de avance.

Proyecto de la asignatura **DSY1105 Desarrollo de Aplicaciones Móviles** (Duoc UC), Evaluación Parcial 2. Es un MVP académico: no se publica en Google Play y trabaja solo con datos ficticios.

## Integrantes

| Integrante | GitHub | Responsabilidades principales |
| --- | --- | --- |
| Simón Inostroza | `simoninos` | Mapa, detalle del punto (esfera, estado y comentarios), lista del modo administrador, aviso sin conexión, navegación, tema visual, ubicación GPS, estado de la red y modelo de puntos de interés |
| Gabriel Sandoval | `Sandy-Yera` | Inicio de sesión, registro, recuperar contraseña, configuración, formulario de administrador (crear y editar puntos), contenedor de dependencias, base de datos Room y hash de contraseñas |
| Catalina Lobos | `Shany-Ghost` | Escaneo de QR, lugar encontrado, Mis lugares, progreso del usuario, comentarios (modelo y repositorio), esfera de agua y pruebas unitarias |

## Funcionalidades implementadas

### Cliente
- **Registro** (nombre de usuario, correo, contraseña, repetir contraseña y aceptación de términos) e **inicio de sesión**, con validación en cada campo: el error aparece con ícono y mensaje bajo el campo. Las contraseñas se guardan con hash PBKDF2 y sal aleatoria.
- **Recuperar contraseña**: pantalla informativa (todavía no envía correos; no hay backend).
- **Mapa de Santiago** (OpenStreetMap) con la ubicación del usuario, un círculo de 60 m y los marcadores de los puntos activos, con un color por estado (bloqueado, desbloqueado, completado).
  - Al tocar un marcador sube una tarjeta con nombre, tipo y distancia, que lleva al detalle.
  - Tarjeta del usuario con su puntaje total.
  - Botones para centrar el mapa en el usuario y para activar o desactivar el seguimiento de la ubicación.
  - El mapa conserva su posición al cambiar de pestaña y volver.
- **Escanear QR** con la cámara: el punto se desbloquea solo si el código existe, el punto está activo y el usuario está a 60 m o menos. Al lograrlo se muestra la pantalla "Lugar encontrado" y se suman 5 puntos.
- **Mis lugares**: lista de los lugares desbloqueados con su esfera de avance y estado ("Recién descubierto", "Falta el video", "Completado"), total de puntos, filtros (Todos, En curso, Completados), orden (recientes, nombre, puntos) y cantidad de lugares por descubrir.
- **Detalle del punto**: imagen, descripción, distancia en vivo, estado según el progreso del usuario y esfera de avance (5 de 20 puntos al escanear, 20 de 20 con el video).
  - **Comentarios**: tras desbloquear el lugar se puede publicar un comentario validado (de 1 a 280 caracteres, con contador) y ver los comentarios del lugar del más nuevo al más antiguo.
  - **Video y selfie**: los botones existen, pero la reproducción del video y la selfie quedan para la siguiente entrega. Sin internet, el botón de video muestra el aviso "sin conexión".
- **Configuración** (desde el engranaje, con una animación que se expande): tema Día/Noche para toda la app, seguir la ubicación, mostrar lugares bloqueados, preferencias de notificaciones (solo se guardan) y cerrar sesión.

### Administrador
- **Lista de puntos de interés** con búsqueda, activación y eliminación con confirmación. Un punto solo se puede activar si tiene todos sus datos completos.
- **Crear y editar puntos** con un formulario validado (nombre, tipo, descripción, latitud, longitud, altitud, enlace de más información, video asociado y si está activo), foto desde la galería o la cámara, botón "Usar mi ubicación actual" y generación del código QR con ZXing.
- Se entra desde el engranaje → Configuración → **Administrar puntos** (el botón solo aparece para el usuario administrador).

### Recursos nativos del dispositivo
- **GPS** con Fused Location Provider: ubicación en tiempo real y distancia a cada punto. El permiso se pide al abrir el mapa.
- **Cámara** con CameraX y ML Kit para leer los códigos QR (el permiso se pide al abrir el escáner), y cámara del sistema para la foto de un punto en el modo administrador.
- **Estado de la red** con ConnectivityManager, para avisar cuando no hay conexión.

### Animaciones
- Transiciones entre pantallas: fundido entre pestañas y deslizamiento hacia las pantallas internas.
- Tarjeta del punto que sube desde abajo en el mapa.
- Engranaje que se expande al abrir Configuración.
- Esfera de agua con nivel animado y ola.
- Desplazamiento animado del mapa al centrar o al elegir un punto, y entrada animada de cada comentario nuevo.

## Arquitectura

La app usa **MVVM** con Jetpack Compose:

```
Pantalla (Composable)  ──eventos──▶  ViewModel  ──▶  Repository  ──▶  Room / GPS / Red
        ▲                                │
        └──────── StateFlow<UiState> ────┘
```

- **Model** (`data/model`): entidades del dominio (`PuntoInteres`, `Usuario`, `Preferencias`, `Sesion`, `Capsula`, `ProgresoPunto`, `Comentario`).
- **Repository** (`data/repository`, `data/location`, `data/red`): una interfaz por fuente de datos, con una versión Room o real que usa la app y una versión `Fake…Repository` en memoria para las pruebas.
- **ViewModel** (`ui/viewmodels`): reúne los datos de los repositorios en un único `UiState` y recibe las acciones del usuario como `UiEvent`. Las validaciones están en `ui/validation`, separadas de la interfaz.
- **Screens y Composables** (`ui/screens`, `ui/components`): solo dibujan el estado y avisan los eventos al ViewModel.

### Estructura de carpetas

```
app/src/main/java/com/cookingplastic/triskeledu/
├── MainActivity.kt, TriskeleduApp.kt, AppContainer.kt
├── data/
│   ├── RepositoriosCompartidos.kt, RepositoriosProgreso.kt   Acceso compartido al GPS, al progreso y a los comentarios
│   ├── model/         Entidades del dominio
│   ├── local/         AppDatabase (Room), DAO, convertidores, carga inicial y datos de prueba
│   ├── repository/    Repositorios (interfaz + versión Room + versión Fake)
│   ├── location/      Ubicación GPS
│   ├── red/           Estado de la conexión
│   └── security/      Hash de contraseñas
├── navigation/        Rutas, barra inferior y NavHost
└── ui/
    ├── screens/       Una carpeta por pantalla
    ├── components/    Composables reutilizables (mapa, esfera, QR, botones, diálogos…)
    ├── state/         UiState de cada pantalla
    ├── event/         UiEvent de cada pantalla
    ├── viewmodels/    ViewModels
    ├── validation/    Reglas de validación (formularios, comentario y regla de los 60 m)
    └── theme/         Colores, tipografía y temas Día/Noche (Material 3)
```

### Persistencia local
- **Room (SQLite)**, definida en `data/local/AppDatabase.kt` (versión 2), guarda **usuarios, sesión, preferencias, cápsulas, puntos de interés, progreso y comentarios**.
- La primera vez que se abre la app, `CargaInicial` llena la base con datos ficticios (`DatosPrueba.kt`).
- Todo lo que hace el usuario sigue ahí al cerrar y abrir la app: la sesión, el tema, los lugares escaneados, los puntos, los comentarios y los cambios del administrador.
- `AppContainer` crea la base y los repositorios una sola vez, y cada ViewModel los recibe con su fábrica.

## Tecnologías

| Área | Herramienta |
| --- | --- |
| Lenguaje | Kotlin 2.0 |
| Interfaz | Jetpack Compose, Material 3, Navigation Compose |
| Estado | ViewModel, StateFlow, corrutinas |
| Persistencia | Room 2.8 |
| Mapa | osmdroid 6.1 (OpenStreetMap) |
| Ubicación | Google Play Services Location |
| Cámara y QR | CameraX, ML Kit (lectura), ZXing (generación) |
| Imágenes | Coil 3 |
| Pruebas | JUnit 4 |

## Cómo ejecutar

**Requisitos**
- Android Studio reciente, compatible con Android Gradle Plugin 9.3 (Gradle 9.5 se descarga solo con el wrapper).
- JDK 17 o superior (el que trae Android Studio sirve).
- Emulador o teléfono con **Android 8.0 (API 26)** o superior, con Google Play Services. La app compila contra la API 36.

**Pasos**
1. Clonar el repositorio (la rama principal es `main/components`):
   ```
   git clone https://github.com/simoninos/CodigoUrbano.git
   ```
2. Abrir la carpeta en Android Studio y esperar a que termine la sincronización de Gradle.
3. Elegir el emulador o el teléfono y presionar **Run ▶** (configuración `app`).
4. Aceptar los permisos de **ubicación** y de **cámara** cuando la app los pida.
5. (Opcional) Ejecutar las pruebas unitarias: `./gradlew test` (en Windows, `gradlew test`).

**En el emulador**
- Para ver los puntos de interés cerca, fija la ubicación en Santiago: *Extended Controls (⋯) → Location*, por ejemplo en La Moneda (-33.44294, -70.65388).
- Para probar el aviso sin conexión, activa el modo avión y toca el video en el detalle de un lugar.

**Usuarios de prueba** (contraseña de todos: `Demo1234`)

| Perfil | Correo | Qué muestra |
| --- | --- | --- |
| Cliente con progreso | camila.explora@ejemplo.cl | Un lugar recién descubierto, uno al que le falta el video y uno completado |
| Cliente | tomas.ruta@ejemplo.cl | Un lugar completado y tema noche |
| Cliente nuevo | usuaria.nueva@ejemplo.cl | Sin lugares desbloqueados |
| Administrador | admin@ejemplo.cl | Acceso a "Administrar puntos" desde Configuración |

Si la app se instaló antes de esta versión, desinstálala una vez (o borra sus datos) para que la base se cree de nuevo con estos usuarios.

## Pendiente para la siguiente entrega

- Reproducción del video (Media3) y selfie: hoy los botones solo informan que estarán disponibles.
- Recuperar contraseña por correo y notificaciones push (requieren backend).
- Sincronización con el backend en Spring Boot.

## Flujo de ramas

- `main/components`: rama principal e integración del equipo; cada integrante abre un pull request desde su rama.
- `simon`, `Gabriel`, `Catalina`: ramas de trabajo de cada integrante.