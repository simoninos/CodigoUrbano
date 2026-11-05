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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.ui.components.AvisoUbicacion
import com.cookingplastic.triskeledu.ui.components.BotonCampana
import com.cookingplastic.triskeledu.ui.components.BotonMapa
import com.cookingplastic.triskeledu.ui.components.EstadoUbicacion
import com.cookingplastic.triskeledu.ui.components.MapaOsm
import com.cookingplastic.triskeledu.ui.components.TarjetaUsuario
import com.cookingplastic.triskeledu.ui.components.colorUbicacionUsuario
import com.cookingplastic.triskeledu.ui.components.registrarRespuestaUbicacion
import com.cookingplastic.triskeledu.ui.components.rememberActivarUbicacion
import com.cookingplastic.triskeledu.ui.components.rememberEstadoUbicacion
import com.cookingplastic.triskeledu.ui.event.MapaUiEvent
import com.cookingplastic.triskeledu.ui.state.MapaUiState
import com.cookingplastic.triskeledu.ui.state.PuntoMapa
import com.cookingplastic.triskeledu.ui.theme.ColoresApp
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.MapaViewModel
import java.util.Locale
import kotlin.math.roundToInt

// Pantalla 3 · Mapa: conecta el ViewModel con el contenido
@Composable
fun MapaScreen(
    alVerDetalle: (String) -> Unit,
    alAbrirConfiguracion: () -> Unit,
    // LVL-01: tocar la tarjeta del usuario abre P24
    alVerNivel: () -> Unit = {},
    // CRE-07 y CRE-08: un punto Sin cápsula abre P21 (creador) o su edición (administrador)
    alAsociarCapsula: (String) -> Unit = {},
    alEditarPunto: (String) -> Unit = {},
    // PER-03: la campana abre el historial de notificaciones (P16)
    alAbrirNotificaciones: () -> Unit = {},
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
        registrarRespuestaUbicacion(context, concedido)
        viewModel.onEvent(MapaUiEvent.OnPermisoUbicacion(concedido))
    }

    // EXC-01: sin permiso o con el GPS apagado aparece el aviso. Se revisa al volver a la app; al conceder
    // el permiso (en el diálogo o en Ajustes) el mapa empieza a usar la ubicación sin reiniciar
    val estadoUbicacion = rememberEstadoUbicacion()
    val activarUbicacion = rememberActivarUbicacion { concedido -> viewModel.onEvent(MapaUiEvent.OnPermisoUbicacion(concedido)) }
    LaunchedEffect(estadoUbicacion) {
        if (estadoUbicacion != EstadoUbicacion.SIN_PERMISO) viewModel.onEvent(MapaUiEvent.OnPermisoUbicacion(true))
    }

    // U-05: el diálogo del sistema se muestra una sola vez. rememberSaveable sobrevive al giro
    // y al cambio de pestaña, así el mapa no vuelve a pedirlo si el usuario ya respondió
    var permisoPreguntado by rememberSaveable { mutableStateOf(false) }

    // Al abrir el mapa: si el permiso ya está concedido se usa; si no, se pide la primera vez
    LaunchedEffect(Unit) {
        val concedido = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (concedido) {
            viewModel.onEvent(MapaUiEvent.OnPermisoUbicacion(true))
        } else if (!permisoPreguntado) {
            permisoPreguntado = true
            pedirPermiso.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    MapaContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        alVerDetalle = alVerDetalle,
        alAbrirConfiguracion = alAbrirConfiguracion,
        alVerNivel = alVerNivel,
        alAsociarCapsula = alAsociarCapsula,
        alEditarPunto = alEditarPunto,
        alAbrirNotificaciones = alAbrirNotificaciones,
        estadoUbicacion = estadoUbicacion,
        alActivarUbicacion = { activarUbicacion(estadoUbicacion) }
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
    mostrarMapa: Boolean = true,
    alVerNivel: () -> Unit = {},
    alAsociarCapsula: (String) -> Unit = {},
    alEditarPunto: (String) -> Unit = {},
    alAbrirNotificaciones: () -> Unit = {},
    estadoUbicacion: EstadoUbicacion = EstadoUbicacion.LISTA,
    alActivarUbicacion: () -> Unit = {}
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
                // EXC-01: sin ubicación no se dibuja el usuario ni el círculo de 60 m
                ubicacionUsuario = estado.ubicacionUsuario.takeIf { estadoUbicacion == EstadoUbicacion.LISTA },
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // weight con fill = false: la tarjeta usa solo el ancho que necesita, sin pasar sobre el engranaje
                if (estado.esAdministrador) {
                    // ADM-11: el administrador ve la leyenda con cuántos puntos hay de cada estado
                    LeyendaAdmin(
                        activos = estado.totalLugares,
                        sinCapsula = estado.puntosSinCapsula,
                        inactivos = estado.puntosInactivos,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 12.dp)
                    )
                } else {
                    TarjetaUsuario(
                        nombre = estado.nombreUsuario,
                        puntos = estado.totalPuntos,
                        lugaresEscaneados = estado.lugaresEscaneados,
                        totalLugares = estado.totalLugares,
                        nivel = estado.nivel,
                        alPresionar = alVerNivel,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 12.dp)
                    )
                }
                // RNF-14: campana y engranaje uno sobre otro, así la tarjeta cabe entera en pantallas de 360 dp
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // PER-03: la campana con el contador de no leídas, para todos los roles
                    BotonCampana(sinLeer = estado.notificacionesSinLeer, alPresionar = alAbrirNotificaciones)
                    BotonMapa(
                        icono = Icons.Filled.Settings,
                        descripcion = "Abrir configuración",
                        alPresionar = alAbrirConfiguracion
                    )
                }
            }
            // CRE-07: el creador y el administrador ven cuántos puntos esperan una cápsula
            if (estado.puntosSinCapsula > 0 && !estado.esAdministrador) {
                AvisoSinCapsula(cantidad = estado.puntosSinCapsula)
            }
            // EXC-01: "Activa la ubicación para encontrar lugares", con el botón que la activa
            AvisoUbicacion(estado = estadoUbicacion, alActivar = alActivarUbicacion)
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
                    if (punto.inactivo) {
                        TarjetaInactivo(punto = punto, alEditar = { alEditarPunto(punto.punto.id) })
                    } else if (punto.sinCapsula) {
                        TarjetaSinCapsula(
                            punto = punto,
                            esAdministrador = estado.esAdministrador,
                            alAsociar = { alAsociarCapsula(punto.punto.id) },
                            alEditar = { alEditarPunto(punto.punto.id) }
                        )
                    } else {
                        TarjetaPunto(punto = punto, alVerDetalle = { alVerDetalle(punto.punto.id) })
                    }
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

// ADM-11: leyenda del mapa del administrador con los tres estados (RN-27)
@Composable
private fun LeyendaAdmin(activos: Int, sinCapsula: Int, inactivos: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilaLeyenda(icono = Icons.Filled.Place, color = MaterialTheme.colorScheme.primary, texto = if (activos == 1) "1 activo" else "$activos activos")
                FilaLeyenda(icono = Icons.Outlined.VideocamOff, color = MaterialTheme.colorScheme.secondary, texto = "$sinCapsula sin cápsula")
            }
            FilaLeyenda(icono = Icons.Outlined.VisibilityOff, color = ColoresApp.bloqueado, texto = if (inactivos == 1) "1 inactivo" else "$inactivos inactivos")
            Text("Los inactivos solo los ves tú", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FilaLeyenda(icono: ImageVector, color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(texto, style = MaterialTheme.typography.labelMedium)
    }
}

// ADM-11: tarjeta de un punto Inactivo en el mapa del administrador
@Composable
private fun TarjetaInactivo(punto: PuntoMapa, alEditar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = punto.punto.nombre, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = punto.punto.tipo.descripcion,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                Text(
                    text = "Inactivo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                Text(text = textoDistancia(punto.distanciaMetros), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "Tiene una cápsula publicada. Actívalo para que los clientes lo vean en el mapa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = alEditar,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Editar", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// CRE-07: aviso amarillo con cuántos puntos Sin cápsula hay
@Composable
private fun AvisoSinCapsula(cantidad: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VideocamOff, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (cantidad == 1) "1 punto sin cápsula" else "$cantidad puntos sin cápsula",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(text = "Solo los ven creadores y administradores", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// CRE-07: tarjeta de un punto Sin cápsula. El creador le asocia una cápsula suya (P21);
// el administrador la elige al editar el punto
@Composable
private fun TarjetaSinCapsula(punto: PuntoMapa, esAdministrador: Boolean, alAsociar: () -> Unit, alEditar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = punto.punto.nombre,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = punto.punto.tipo.descripcion,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                Text(
                    text = "Sin cápsula",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                Text(
                    text = textoDistancia(punto.distanciaMetros),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = if (esAdministrador) "Este punto todavía no tiene cápsula. Elige una publicada al editarlo para poder activarlo."
                else "Este punto todavía no tiene cápsula. Asígnale una tuya para que el administrador pueda activarlo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = if (esAdministrador) alEditar else alAsociar,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(if (esAdministrador) Icons.Outlined.Edit else Icons.Outlined.Videocam, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (esAdministrador) "Editar punto" else "Asociar cápsula", style = MaterialTheme.typography.labelLarge)
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
