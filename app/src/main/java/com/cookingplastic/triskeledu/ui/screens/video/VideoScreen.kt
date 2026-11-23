package com.cookingplastic.triskeledu.ui.screens.video

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.PUNTOS_CAPSULA
import com.cookingplastic.triskeledu.ui.event.VideoUiEvent
import com.cookingplastic.triskeledu.ui.state.ModoReproductor
import com.cookingplastic.triskeledu.ui.state.VideoUiState
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.VideoViewModel
import kotlinx.coroutines.delay

//P12: se conecta al ViewModel y es dueña del ExoPlayer, que necesita el Context y se libera al salir
@Composable
fun VideoScreen(
    alVolver: () -> Unit,
    viewModel: VideoViewModel = viewModel(factory = VideoViewModel.Factory),
    //P18-B agrega abajo el aviso del enlace y los botones Volver y Publicar
    pie: (@Composable () -> Unit)? = null
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val enlace = estado.enlace

    //íconos claros en la barra de estado mientras se ve el video, aunque la app esté de día
    val vista = LocalView.current
    val actividad = LocalActivity.current
    DisposableEffect(actividad) {
        val controlador = actividad?.window?.let { WindowCompat.getInsetsController(it, vista) }
        val anterior = controlador?.isAppearanceLightStatusBars
        controlador?.isAppearanceLightStatusBars = false
        onDispose { if (anterior != null) controlador?.isAppearanceLightStatusBars = anterior }
    }

    //el reproductor siempre se ve oscuro, como en el mockup, sin importar el tema elegido
    TriskeleduTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            if (enlace != null && !estado.cargando && estado.error == null) {
                val reproductor = rememberReproductor(enlace, estado, viewModel::onEvent)
                VideoContent(
                    estado = estado,
                    onEvent = viewModel::onEvent,
                    //solo en modo libre aparece la barra que llama a esto
                    alBuscar = { ms -> reproductor.seekTo(ms) },
                    alVolver = alVolver,
                    pie = pie,
                    superficie = { modificador ->
                        AndroidView(
                            factory = { contexto ->
                                PlayerView(contexto).apply {
                                    //sin controles propios: así no hay gestos para adelantar en modo bloqueado
                                    useController = false
                                    keepScreenOn = true
                                    player = reproductor
                                }
                            },
                            modifier = modificador
                        )
                    }
                )
            } else {
                VideoContent(estado = estado, onEvent = viewModel::onEvent, alBuscar = {}, alVolver = alVolver, pie = pie)
            }
        }
    }
}

//Crea el ExoPlayer, le avisa al ViewModel lo que pasa y lo pausa cuando la app pasa a segundo plano
@Composable
private fun rememberReproductor(enlace: String, estado: VideoUiState, onEvent: (VideoUiEvent) -> Unit): ExoPlayer {
    val contexto = LocalContext.current
    val reproductor = remember(enlace) { ExoPlayer.Builder(contexto).build() }
    val avisar by rememberUpdatedState(onEvent)

    DisposableEffect(reproductor) {
        val oyente = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) avisar(VideoUiEvent.OnTerminada)
            }

            override fun onPlayerError(error: PlaybackException) {
                val deRed = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                avisar(VideoUiEvent.OnErrorReproduccion(esDeRed = deRed))
            }
        }
        reproductor.addListener(oyente)
        onDispose {
            //guarda dónde iba antes de liberar: al girar el teléfono se retoma desde ahí (RNF-07)
            avisar(VideoUiEvent.OnAvance(reproductor.currentPosition, reproductor.duration.coerceAtLeast(0)))
            reproductor.removeListener(oyente)
            reproductor.release()
        }
    }

    //carga el video al abrir y en cada "Reintentar" o "Ver de nuevo", desde donde había quedado
    LaunchedEffect(reproductor, estado.intento) {
        reproductor.setMediaItem(MediaItem.fromUri(enlace))
        reproductor.prepare()
        reproductor.seekTo(estado.posicionMs)
    }

    //reproduce o pausa según el estado
    val debeSonar = estado.reproduciendo && !estado.sinConexion && !estado.terminada
    LaunchedEffect(reproductor, debeSonar) { reproductor.playWhenReady = debeSonar }

    //avisa el avance cada medio segundo para la barra y para retomar después
    LaunchedEffect(reproductor) {
        while (true) {
            avisar(VideoUiEvent.OnAvance(reproductor.currentPosition, reproductor.duration.coerceAtLeast(0)))
            delay(500)
        }
    }

    //RN-08: en segundo plano se pausa, y al volver retoma si estaba sonando
    val ciclo = LocalLifecycleOwner.current
    DisposableEffect(ciclo, reproductor) {
        var seguia = false
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_STOP -> {
                    seguia = reproductor.playWhenReady
                    reproductor.pause()
                }
                Lifecycle.Event.ON_START -> if (seguia) reproductor.play()
                else -> Unit
            }
        }
        ciclo.lifecycle.addObserver(observador)
        onDispose { ciclo.lifecycle.removeObserver(observador) }
    }
    return reproductor
}

//Solo dibuja; superficie es el video (en las vistas previas queda vacía)
@Composable
fun VideoContent(
    estado: VideoUiState,
    onEvent: (VideoUiEvent) -> Unit,
    alBuscar: (Long) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
    pie: (@Composable () -> Unit)? = null,
    superficie: (@Composable (Modifier) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Encabezado(estado = estado, alVolver = alVolver)
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                estado.cargando -> CircularProgressIndicator()
                estado.error != null -> Aviso(
                    titulo = estado.error,
                    texto = null,
                    boton = "Volver",
                    alPresionar = alVolver
                )
                //RN-35: sin enlace no hay video, solo el aviso
                estado.sinEnlace -> AvisoSinEnlace(estado = estado, alVolver = alVolver)
                else -> {
                    superficie?.invoke(Modifier.fillMaxSize())
                    when {
                        estado.sinConexion -> Aviso(
                            titulo = "Se perdió la conexión",
                            texto = "El video sigue desde donde quedó.",
                            boton = "Reintentar",
                            icono = { Icon(Icons.Filled.WifiOff, contentDescription = null) },
                            alPresionar = { onEvent(VideoUiEvent.OnReintentar) }
                        )
                        estado.terminada -> AvisoTerminada(estado = estado, onEvent = onEvent, alVolver = alVolver)
                        else -> BotonPausa(
                            reproduciendo = estado.reproduciendo,
                            alPresionar = { onEvent(VideoUiEvent.OnReproduciendo(!estado.reproduciendo)) }
                        )
                    }
                }
            }
        }

        if (!estado.cargando && estado.error == null && !estado.sinEnlace) {
            Spacer(modifier = Modifier.height(12.dp))
            BarraAvance(estado = estado, alBuscar = alBuscar)
            Spacer(modifier = Modifier.height(12.dp))
            NotaModo(estado = estado)
        }
        pie?.let {
            Spacer(modifier = Modifier.height(12.dp))
            it()
        }
    }
}

@Composable
private fun Encabezado(estado: VideoUiState, alVolver: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            IconButton(onClick = alVolver) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar la cápsula")
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = estado.capsula?.titulo ?: "Cápsula",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            val subtitulo = listOfNotNull(estado.nombrePunto, estado.capsula?.categoria?.etiqueta)
            if (subtitulo.isNotEmpty()) {
                Text(
                    text = subtitulo.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BotonPausa(reproduciendo: Boolean, alPresionar: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f), CircleShape)
            .clickable(onClick = alPresionar),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (reproduciendo) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (reproduciendo) "Pausar" else "Reproducir",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(40.dp)
        )
    }
}

//RN-08: en modo bloqueado la barra solo muestra el avance; en libre se puede arrastrar
@Composable
private fun BarraAvance(estado: VideoUiState, alBuscar: (Long) -> Unit) {
    val duracion = estado.duracionMs.coerceAtLeast(1)
    var arrastrando by remember { mutableStateOf<Float?>(null) }
    val avance = arrastrando ?: (estado.posicionMs.toFloat() / duracion).coerceIn(0f, 1f)

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(textoTiempo((avance * duracion).toLong()), style = MaterialTheme.typography.bodyMedium)
            Text(textoTiempo(estado.duracionMs), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (estado.modo == ModoReproductor.LIBRE) {
            Slider(
                value = avance,
                onValueChange = { arrastrando = it },
                onValueChangeFinished = {
                    arrastrando?.let { alBuscar((it * duracion).toLong()) }
                    arrastrando = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LinearProgressIndicator(
                progress = { avance },
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }
    }
}

@Composable
private fun NotaModo(estado: VideoUiState) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (estado.modo == ModoReproductor.BLOQUEADO) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            val texto = when {
                estado.esVistaPrevia -> buildAnnotatedString { append("Vista previa: puedes adelantar y no suma puntos.") }
                estado.modo == ModoReproductor.BLOQUEADO -> buildAnnotatedString {
                    append("No se puede adelantar. Al terminar ganas ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)) {
                        append("+$PUNTOS_CAPSULA puntos")
                    }
                    append(".")
                }
                else -> buildAnnotatedString { append("Ya viste esta cápsula: puedes adelantar y retroceder.") }
            }
            Text(text = texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AvisoTerminada(estado: VideoUiState, onEvent: (VideoUiEvent) -> Unit, alVolver: () -> Unit) {
    val texto = when {
        estado.esVistaPrevia -> "La vista previa no suma puntos."
        estado.puntosGanados > 0 -> "Ganaste +${estado.puntosGanados} puntos."
        else -> "Los puntos de esta cápsula ya estaban sumados."
    }
    Aviso(
        titulo = "¡Cápsula completa!",
        texto = texto,
        boton = "Volver",
        alPresionar = alVolver,
        botonSecundario = "Ver de nuevo",
        alPresionarSecundario = { onEvent(VideoUiEvent.OnVerDeNuevo) }
    )
}

@Composable
private fun AvisoSinEnlace(estado: VideoUiState, alVolver: () -> Unit) {
    val texto = when {
        estado.esVistaPrevia -> "La vista previa no suma puntos."
        estado.puntosGanados > 0 -> "Ganaste +${estado.puntosGanados} puntos."
        else -> "Los $PUNTOS_CAPSULA puntos de esta cápsula ya se habían sumado."
    }
    Aviso(titulo = "Video no enlazado: simulación de vista hecha", texto = texto, boton = "Volver", alPresionar = alVolver)
}

@Composable
private fun Aviso(
    titulo: String,
    texto: String?,
    boton: String,
    alPresionar: () -> Unit,
    icono: (@Composable () -> Unit)? = null,
    botonSecundario: String? = null,
    alPresionarSecundario: () -> Unit = {}
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 8.dp)) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icono?.invoke()
            Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            if (texto != null) {
                Text(texto, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (botonSecundario != null) {
                    OutlinedButton(onClick = alPresionarSecundario, modifier = Modifier.height(48.dp)) { Text(botonSecundario) }
                }
                Button(onClick = alPresionar, modifier = Modifier.height(48.dp).width(140.dp)) { Text(boton) }
            }
        }
    }
}

//65000 -> "1:05"
fun textoTiempo(ms: Long): String {
    val segundos = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(segundos / 60, segundos % 60)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VideoBloqueadoPreview() {
    TriskeleduTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            VideoContent(
                estado = VideoUiState(
                    capsula = DatosPrueba.CAPSULA_LA_MONEDA,
                    nombrePunto = "La Moneda",
                    enlace = DatosPrueba.CLIP_PRUEBA,
                    posicionMs = 12_000,
                    duracionMs = 20_000,
                    cargando = false
                ),
                onEvent = {},
                alBuscar = {},
                alVolver = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VideoSinEnlacePreview() {
    TriskeleduTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            VideoContent(
                estado = VideoUiState(
                    capsula = DatosPrueba.CAPSULA_CATEDRAL,
                    nombrePunto = "Catedral Metropolitana de Santiago",
                    sinEnlace = true,
                    modo = ModoReproductor.LIBRE,
                    puntosGanados = PUNTOS_CAPSULA,
                    terminada = true,
                    cargando = false
                ),
                onEvent = {},
                alBuscar = {},
                alVolver = {}
            )
        }
    }
}
