package com.cookingplastic.triskeledu.ui.screens.mapa

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.ui.components.BotonMapa
import com.cookingplastic.triskeledu.ui.components.MapaOsm
import com.cookingplastic.triskeledu.ui.components.TarjetaUsuario
import com.cookingplastic.triskeledu.ui.components.colorUbicacionUsuario
import com.cookingplastic.triskeledu.ui.event.MapaUiEvent
import com.cookingplastic.triskeledu.ui.state.MapaUiState
import com.cookingplastic.triskeledu.ui.state.PuntoMapa
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.MapaViewModel
import java.util.Locale
import kotlin.math.roundToInt

// Pantalla 3 · Mapa: conecta el ViewModel con el contenido
@Composable
fun MapaScreen(
    alVerDetalle: (String) -> Unit,
    alAbrirConfiguracion: () -> Unit,
    viewModel: MapaViewModel = viewModel(factory = MapaViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // U-03: diálogo del sistema que pide la ubicación; la respuesta se avisa al ViewModel
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { respuestas ->
        val concedido = respuestas[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            respuestas[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onEvent(MapaUiEvent.OnPermisoUbicacion(concedido))
    }

    // Al abrir el mapa: si el permiso ya está concedido se usa; si no, se pide
    LaunchedEffect(Unit) {
        val concedido = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (concedido) {
            viewModel.onEvent(MapaUiEvent.OnPermisoUbicacion(true))
        } else {
            pedirPermiso.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    MapaContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        alVerDetalle = alVerDetalle,
        alAbrirConfiguracion = alAbrirConfiguracion
    )
}

// Solo dibuja: recibe el estado y avisa los eventos, por eso funciona en Preview sin ViewModel.
// mostrarMapa = false omite el mapa de osmdroid, que no se dibuja en los Preview.
@Composable
fun MapaContent(
    estado: MapaUiState,
    onEvent: (MapaUiEvent) -> Unit,
    alVerDetalle: (String) -> Unit,
    alAbrirConfiguracion: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarMapa: Boolean = true
) {
    val snackbar = remember { SnackbarHostState() }

    // M-18: los errores se muestran como mensaje
    LaunchedEffect(estado.mensajeError) {
        estado.mensajeError?.let {
            snackbar.showSnackbar(it)
            onEvent(MapaUiEvent.OnErrorMostrado)
        }
    }

    // M-11: con la tarjeta abierta, el botón atrás la cierra en vez de salir del mapa
    BackHandler(enabled = estado.seleccionadoId != null) {
        onEvent(MapaUiEvent.OnCerrarTarjeta)
    }

    // Se recuerda el último punto elegido para que la tarjeta no quede vacía mientras baja
    val seleccionado = estado.seleccionado
    var ultimoSeleccionado by remember { mutableStateOf<PuntoMapa?>(null) }
    LaunchedEffect(seleccionado) { if (seleccionado != null) ultimoSeleccionado = seleccionado }
    val puntoTarjeta = seleccionado ?: ultimoSeleccionado

    Box(modifier = modifier.fillMaxSize()) {
        if (mostrarMapa) {
            MapaOsm(
                puntos = estado.puntosVisibles,
                ubicacionUsuario = estado.ubicacionUsuario,
                seleccionadoId = estado.seleccionadoId,
                seguirUbicacion = estado.seguirUbicacion,
                solicitudCentrar = estado.solicitudCentrar,
                camaraInicial = estado.camara,
                alTocarPunto = { onEvent(MapaUiEvent.OnClickPunto(it)) },
                alTocarMapa = { onEvent(MapaUiEvent.OnCerrarTarjeta) },
                alGuardarCamara = { onEvent(MapaUiEvent.OnCamaraGuardada(it)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // M-15: tarjeta del usuario a la izquierda y engranaje a la derecha
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // weight con fill = false: la tarjeta usa solo el ancho que necesita, sin pasar sobre el engranaje
            TarjetaUsuario(
                nombre = estado.nombreUsuario,
                puntos = estado.totalPuntos,
                lugaresEscaneados = estado.lugaresEscaneados,
                totalLugares = estado.totalLugares,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 12.dp)
            )
            BotonMapa(
                icono = Icons.Filled.Settings,
                descripcion = "Abrir configuración",
                alPresionar = alAbrirConfiguracion
            )
        }

        if (estado.cargando) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Abajo: botón "Centrar", tarjeta del punto y mensajes
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // M-20: seguimiento activo = flecha llena azul; inactivo = flecha de contorno gris
            BotonMapa(
                icono = if (estado.seguirUbicacion) Icons.Filled.Navigation else Icons.Outlined.Navigation,
                descripcion = if (estado.seguirUbicacion) "Desactivar seguimiento" else "Activar seguimiento",
                alPresionar = { onEvent(MapaUiEvent.OnAlternarSeguimiento) },
                colorIcono = if (estado.seguirUbicacion) colorUbicacionUsuario()
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            BotonMapa(
                icono = Icons.Filled.MyLocation,
                descripcion = "Centrar en mi ubicación",
                alPresionar = { onEvent(MapaUiEvent.OnCentrar) },
                colorIcono = colorUbicacionUsuario()
            )
            // M-09: la tarjeta sube desde abajo al tocar un marcador
            AnimatedVisibility(
                visible = seleccionado != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                puntoTarjeta?.let { punto ->
                    TarjetaPunto(punto = punto, alVerDetalle = { alVerDetalle(punto.punto.id) })
                }
            }
            SnackbarHost(hostState = snackbar)
        }

        // M-19: atribución de OpenStreetMap
        Text(
            text = "© OpenStreetMap",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 4.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

// Tarjeta del punto seleccionado (M-09, M-10, M-12)
@Composable
private fun TarjetaPunto(punto: PuntoMapa, alVerDetalle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = punto.punto.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Etiqueta con el tipo del punto
                    Text(
                        text = punto.punto.tipo.descripcion,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "${textoDistancia(punto.distanciaMetros)} · ${punto.estado.etiqueta}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Button(
                onClick = alVerDetalle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Ver detalle", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// M-10: "a 35 m" bajo 1 km, "a 1,2 km" desde 1 km, "Calculando distancia…" sin ubicación
private fun textoDistancia(metros: Double?): String = when {
    metros == null -> "Calculando distancia…"
    metros < 1000 -> "a ${metros.roundToInt()} m"
    else -> "a " + String.format(Locale("es", "CL"), "%.1f", metros / 1000) + " km"
}

private val estadoPreview = MapaUiState(
    puntos = DatosPrueba.puntos.filter { it.activo }.map {
        PuntoMapa(punto = it, estado = EstadoPunto.BLOQUEADO, distanciaMetros = 35.0)
    },
    seleccionadoId = DatosPrueba.PUNTO_LA_MONEDA.id,
    nombreUsuario = "cliente_prueba",
    cargando = false
)

@Preview(showBackground = true, widthDp = 390, heightDp = 760)
@Composable
private fun MapaPreview() {
    TriskeleduTheme {
        MapaContent(
            estado = estadoPreview,
            onEvent = {},
            alVerDetalle = {},
            alAbrirConfiguracion = {},
            mostrarMapa = false
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 760)
@Composable
private fun MapaNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        MapaContent(
            estado = estadoPreview,
            onEvent = {},
            alVerDetalle = {},
            alAbrirConfiguracion = {},
            mostrarMapa = false
        )
    }
}
