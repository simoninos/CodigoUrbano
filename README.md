# Aprendo IA Jugando en Santiago

Aplicación Android para **Triskeledu SpA** que convierte el recorrido por Santiago en un juego para aprender sobre inteligencia artificial. El usuario explora un mapa de la ciudad, se acerca a puntos de interés, escanea el código QR del lugar para desbloquearlo, ve cápsulas de video sobre IA y acumula puntos.

Proyecto de la asignatura **DSY1105 Desarrollo de Aplicaciones Móviles** (Duoc UC). Es un MVP académico: no se publica en Google Play y trabaja solo con datos ficticios.

## Integrantes

| Integrante | Responsabilidades principales |
| --- | --- |
| Simón Inostroza | Mapa, detalle del punto, lista del modo administrador, aviso sin conexión, navegación, tema visual, ubicación GPS y modelo de puntos de interés |
| Gabriel _(apellido)_ | Inicio de sesión, registro, recuperar contraseña, configuración, edición de puntos (administrador), contenedor de dependencias y base de datos Room |
| Catalina _(apellido)_ | Escaneo de QR, lugar encontrado, mis lugares, progreso del usuario y comentarios |

## Funcionalidades

### Cliente
- **Registro e inicio de sesión** con formularios validados: cada campo muestra un ícono y un mensaje cuando el dato no es válido. Las contraseñas se guardan con hash PBKDF2 y sal aleatoria.
- **Mapa de Santiago** (OpenStreetMap) con la ubicación del usuario, un círculo de 60 m y los marcadores de los puntos de interés activos.
  - Al tocar un marcador aparece una tarjeta con nombre, tipo y distancia.
  - Botones para centrar el mapa en el usuario y para activar o desactivar el seguimiento de la ubicación.
  - El mapa conserva su posición al cambiar de pestaña y volver.
- **Detalle del punto**: imagen, descripción, distancia en vivo y actividades. Sin internet, el video muestra un aviso de "sin conexión".
- **Escaneo de QR** con la cámara para desbloquear un punto, solo si el usuario está a 60 m o menos.
- **Configuración**: tema Día/Noche, seguir la ubicación, mostrar puntos bloqueados y cerrar sesión. Se abre desde el engranaje con una animación.

### Administrador
- **Lista de puntos de interés** con búsqueda, activación y eliminación con confirmación. Un punto solo se puede activar si tiene todos sus datos completos.
- **Crear y editar puntos** con un formulario validado y la generación del código QR.

### Recursos nativos del dispositivo
- **GPS** con Fused Location Provider: ubicación en tiempo real y distancia a cada punto. El permiso se pide al abrir el mapa.
- **Cámara** con CameraX y ML Kit para leer los códigos QR. El permiso se pide al abrir el escáner.
- **Estado de la red** con ConnectivityManager, para avisar cuando no hay conexión.

### Animaciones
- Transiciones entre pantallas: fundido entre pestañas y deslizamiento hacia las pantallas internas.
- Tarjeta del punto que sube desde abajo en el mapa.
- Animación del engranaje que se expande al abrir Configuración.
- Desplazamiento animado del mapa al centrar o al elegir un punto.

## Arquitectura

La app usa **MVVM** con Jetpack Compose:

```
Pantalla (Composable)  ──eventos──▶  ViewModel  ──▶  Repository  ──▶  Room / GPS / Red
        ▲                                │
        └──────── StateFlow<UiState> ────┘
```

- **Model** (`data/model`): las entidades del dominio (`PuntoInteres`, `Usuario`, `Preferencias`, `Capsula`, etc.).
- **Repository** (`data/repository`, `data/location`, `data/red`): una interfaz por fuente de datos, con una versión real y una versión de prueba (`Fake…Repository`).
- **ViewModel** (`ui/viewmodels`): reúne los datos de los repositorios en un único `UiState` y recibe las acciones del usuario como `UiEvent`. Las validaciones están en `ui/validation`, separadas de la interfaz.
- **Screens y Composables** (`ui/screens`, `ui/components`): solo dibujan el estado y avisan los eventos al ViewModel.

### Estructura de carpetas

```
app/src/main/java/com/cookingplastic/triskeledu/
├── MainActivity.kt, TriskeleduApp.kt, AppContainer.kt
├── data/
│   ├── model/         Entidades del dominio
│   ├── local/         AppDatabase (Room), DAO, convertidores y datos de prueba
│   ├── repository/    Repositorios (interfaz + versión Room/real + versión Fake)
│   ├── location/      Ubicación GPS
│   ├── red/           Estado de la conexión
│   └── security/      Hash de contraseñas
├── navigation/        Rutas, barra inferior y NavHost
└── ui/
    ├── screens/       Una carpeta por pantalla
    ├── components/    Composables reutilizables (mapa, botones, diálogos…)
    ├── state/         UiState de cada pantalla
    ├── event/         UiEvent de cada pantalla
    ├── viewmodels/    ViewModels
    ├── validation/    Reglas de validación de formularios
    └── theme/         Colores, tipografía y temas Día/Noche (Material 3)
```

### Persistencia local
La base de datos es **Room (SQLite)** y se define en `data/local/AppDatabase.kt`. Guarda usuarios, sesión, preferencias, cápsulas y puntos de interés. La primera vez que se abre la app se carga con datos ficticios (`DatosPrueba.kt`).

## Tecnologías

| Área | Herramienta |
| --- | --- |
| Lenguaje | Kotlin 2.0 |
| Interfaz | Jetpack Compose, Material 3, Navigation Compose |
| Estado | ViewModel, StateFlow, corrutinas |
| Persistencia | Room 2.8 |
| Mapa | osmdroid 6.1 (OpenStreetMap) |
| Ubicación | Google Play Services Location |
| Cámara y QR | CameraX, ML Kit, ZXing |
| Imágenes | Coil 3 |

## Cómo ejecutar

**Requisitos**
- Android Studio (una versión reciente, compatible con Android Gradle Plugin 9.3).
- JDK 17 o superior (el que trae Android Studio sirve).
- Emulador o teléfono con **Android 8.0 (API 26)** o superior, con Google Play Services. La app compila contra la API 36.

**Pasos**
1. Clonar el repositorio:
   ```
   git clone https://github.com/simoninos/CodigoUrbano.git
   ```
2. Abrir la carpeta en Android Studio y esperar a que termine la sincronización de Gradle.
3. Elegir el emulador o el teléfono y presionar **Run ▶** (configuración `app`).
4. Aceptar los permisos de **ubicación** y de **cámara** cuando la app los pida.

**En el emulador**
- Para ver los puntos de interés cerca, fija la ubicación en Santiago: *Extended Controls (⋯) → Location*, por ejemplo en La Moneda (-33.4429, -70.6539).
- Para probar el aviso sin conexión, activa el modo avión.

**Usuarios de prueba**

| Perfil | Correo | Contraseña |
| --- | --- | --- |
| Cliente | cliente@triskeledu.cl | Cliente123 |
| Administrador | admin@triskeledu.cl | Admin1234 |

Al modo administrador se entra desde el engranaje → Configuración → Modo administrador (solo con el usuario administrador).

## Flujo de ramas

- `master`: versión entregable.
- `main/components`: integración del equipo; cada integrante abre un pull request desde su rama.
- `simon`, `Gabriel`, `Catalina`: ramas de trabajo de cada integrante.

La planificación y el reparto de tareas están en el tablero de Trello del equipo: _(enlace)_.
