package com.cookingplastic.triskeledu.ui.screens.detalle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.cookingplastic.triskeledu.R
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.ui.components.DialogoSinConexion
import com.cookingplastic.triskeledu.ui.components.EsferaAgua
import com.cookingplastic.triskeledu.ui.event.DetalleUiEvent
import com.cookingplastic.triskeledu.ui.state.DetalleUiState
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.validation.ValidacionComentario
import com.cookingplastic.triskeledu.ui.viewmodels.DetalleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Pantalla 4 · Detalle del punto: conecta el ViewModel con el contenido
@Composable
fun DetalleScreen(
    alVolver: () -> Unit,
    alAbrirConfiguracion: () -> Unit,
    viewModel: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    DetalleContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        alVolver = alVolver,
        alAbrirConfiguracion = alAbrirConfiguracion
    )
}

@Composable
fun DetalleContent(
    estado: DetalleUiState,
    onEvent: (DetalleUiEvent) -> Unit,
    alVolver: () -> Unit,
    alAbrirConfiguracion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbar = remember { SnackbarHostState() }
    val abridorEnlaces = LocalUriHandler.current

    // D-14
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            onEvent(DetalleUiEvent.OnMensajeMostrado)
        }
    }

    // Pantalla 11
    if (estado.mostrarSinConexion) {
        DialogoSinConexion(
            lugarDesbloqueado = estado.desbloqueado,
            alCerrar = { onEvent(DetalleUiEvent.OnCerrarSinConexion) }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val punto = estado.punto
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // D-02: volver a la izquierda y engranaje a la derecha
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BotonCabecera(Icons.AutoMirrored.Filled.ArrowBack, "Volver al mapa", alVolver)
                    BotonCabecera(Icons.Filled.Settings, "Abrir configuración", alAbrirConfiguracion)
                }
            }

            when {
                estado.cargando -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                // D-01
                punto == null -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Este lugar ya no existe", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = alVolver, shape = RoundedCornerShape(16.dp)) { Text("Volver al mapa") }
                    }
                }

                else -> {
                    // D-04: imagen con esquinas redondeadas
                    item {
                        ImagenPunto(
                            imagenUri = punto.imagenUri,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }

                    // D-05: nombre, tipo y distancia en vivo; D-15: esfera de avance a la derecha
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = punto.nombre,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = punto.tipo.descripcion,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        text = textoDistancia(estado.distanciaMetros),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                            EsferaAgua(
                                puntos = estado.puntosLugar,
                                maximo = estado.puntosMaximos,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    // D-06 y D-07
                    if (!punto.descripcion.isNullOrBlank() || punto.descubreMasUrl != null) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!punto.descripcion.isNullOrBlank()) {
                                    Text("Descripción", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = punto.descripcion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                punto.descubreMasUrl?.let { url ->
                                    TextButton(
                                        onClick = {
                                            try {
                                                abridorEnlaces.openUri(url)
                                            } catch (e: Exception) {
                                                onEvent(DetalleUiEvent.OnEnlaceNoDisponible)
                                            }
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Descubre más", style = MaterialTheme.typography.labelLarge)
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // D-08 y D-09: actividades
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Actividades", style = MaterialTheme.typography.titleSmall)

                            if (estado.desbloqueado) {
                                Actividad(
                                    icono = Icons.Filled.Check,
                                    fondoIcono = MaterialTheme.colorScheme.primary,
                                    colorIcono = MaterialTheme.colorScheme.onPrimary,
                                    titulo = "QR escaneado",
                                    detalle = "+5 pts · Completado",
                                    fondo = MaterialTheme.colorScheme.primaryContainer,
                                    borde = null
                                )
                            } else {
                                Actividad(
                                    icono = Icons.Filled.QrCodeScanner,
                                    titulo = "Escanea el QR del lugar",
                                    detalle = "+5 pts · Pendiente"
                                )
                            }
                            if (estado.videoVisto) {
                                Actividad(
                                    icono = Icons.Filled.Check,
                                    fondoIcono = MaterialTheme.colorScheme.primary,
                                    colorIcono = MaterialTheme.colorScheme.onPrimary,
                                    titulo = "Video visto",
                                    detalle = listOfNotNull(estado.capsula?.titulo, "+15 pts · Completado").joinToString(" · "),
                                    fondo = MaterialTheme.colorScheme.primaryContainer,
                                    borde = null
                                )
                            } else {
                                Actividad(
                                    icono = Icons.Filled.PlayArrow,
                                    fondoIcono = MaterialTheme.colorScheme.secondary,
                                    colorIcono = MaterialTheme.colorScheme.onSecondary,
                                    // D-21: título y duración de la cápsula del punto
                                    titulo = estado.capsula?.titulo ?: "Mira el video sobre IA",
                                    detalle = listOfNotNull(
                                        "+15 pts",
                                        estado.capsula?.duracionSegundos?.let { textoDuracion(it) },
                                        "Hay que verlo completo"
                                    ).joinToString(" · "),
                                    borde = MaterialTheme.colorScheme.primary,
                                    alPresionar = { onEvent(DetalleUiEvent.OnVerVideo) }
                                )
                            }
                            Actividad(
                                icono = Icons.Filled.PhotoCamera,
                                titulo = "Sube tu selfie",
                                detalle = "Evidencia de tu visita",
                                alPresionar = { onEvent(DetalleUiEvent.OnTomarSelfie) }
                            )
                            Actividad(
                                icono = Icons.Filled.ChatBubbleOutline,
                                titulo = "Comenta algo interesante",
                                detalle = "Se guarda en tu teléfono",
                                alPresionar = { onEvent(DetalleUiEvent.OnComentar) }
                            )
                            // D-16: el formulario se despliega bajo la actividad
                            AnimatedVisibility(
                                visible = estado.formularioComentarioAbierto,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                FormularioComentario(estado = estado, onEvent = onEvent)
                            }
                        }
                    }

                    // D-20: comentarios del lugar, del más nuevo al más antiguo
                    if (estado.comentarios.isNotEmpty()) {
                        item {
                            Text(
                                text = "Comentarios (${estado.comentarios.size})",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        // key permite que cada comentario nuevo entre con animación y no redibuje los demás
                        items(estado.comentarios, key = { it.id }) { comentario ->
                            TarjetaComentario(
                                comentario = comentario,
                                esMio = comentario.usuarioId == estado.usuarioId,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

// Botón cuadrado de 48 dp de la cabecera (D-02)
@Composable
private fun BotonCabecera(icono: ImageVector, descripcion: String, alPresionar: () -> Unit) {
    IconButton(
        onClick = alPresionar,
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Icon(icono, contentDescription = descripcion)
    }
}

// D-04: la imagen de prueba y las que no cargan se reemplazan por la imagen local
@Composable
private fun ImagenPunto(imagenUri: String?, modifier: Modifier = Modifier) {
    val reemplazo = painterResource(R.drawable.punto_placeholder)
    if (imagenUri.isNullOrBlank() || imagenUri == DatosPrueba.IMAGEN_PRUEBA) {
        Image(
            painter = reemplazo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Coil carga la imagen desde el teléfono sin bloquear la pantalla
        AsyncImage(
            model = imagenUri,
            contentDescription = null,
            placeholder = reemplazo,
            error = reemplazo,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

// Fila de una actividad: ícono en un cuadro de color, título, detalle y flecha si se puede tocar
@Composable
private fun Actividad(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    fondoIcono: Color = MaterialTheme.colorScheme.surfaceVariant,
    colorIcono: Color = MaterialTheme.colorScheme.onSurface,
    fondo: Color = MaterialTheme.colorScheme.surface,
    borde: Color? = MaterialTheme.colorScheme.outline,
    alPresionar: (() -> Unit)? = null
) {
    val contenido: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(fondoIcono, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = colorIcono)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(detalle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (alPresionar != null) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    val forma = RoundedCornerShape(16.dp)
    val bordeTrazo = borde?.let { BorderStroke(2.dp, it) }
    if (alPresionar != null) {
        Surface(onClick = alPresionar, shape = forma, color = fondo, border = bordeTrazo, modifier = Modifier.fillMaxWidth()) {
            contenido()
        }
    } else {
        Surface(shape = forma, color = fondo, border = bordeTrazo, modifier = Modifier.fillMaxWidth()) {
            contenido()
        }
    }
}

// Formulario de comentario (D-17 y D-18): error con ícono bajo el campo, contador de caracteres
// y "Publicar" deshabilitado mientras el texto no sea válido
@Composable
private fun FormularioComentario(estado: DetalleUiState, onEvent: (DetalleUiEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = estado.textoComentario,
            onValueChange = { onEvent(DetalleUiEvent.OnTextoComentarioChange(it)) },
            label = { Text("Tu comentario") },
            minLines = 3,
            maxLines = 6,
            isError = estado.errorComentario != null,
            trailingIcon = {
                if (estado.errorComentario != null) {
                    Icon(Icons.Filled.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                }
            },
            supportingText = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = estado.errorComentario ?: "",
                        modifier = Modifier.weight(1f)
                    )
                    Text("${estado.textoComentario.trim().length}/${ValidacionComentario.MAXIMO_CARACTERES}")
                }
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { onEvent(DetalleUiEvent.OnComentar) }) { Text("Cancelar") }
            Button(
                onClick = { onEvent(DetalleUiEvent.OnPublicarComentario) },
                enabled = estado.puedePublicarComentario,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp)
            ) {
                if (estado.guardandoComentario) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Publicar", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// Un comentario guardado: autor, fecha y texto
@Composable
private fun TarjetaComentario(comentario: Comentario, esMio: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (esMio) "Tú" else "Otro explorador",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = textoFecha(comentario.creadoEn),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = comentario.texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Duración de la cápsula: "58 s" bajo un minuto, "1 min" o "1 min 30 s" desde un minuto
private fun textoDuracion(segundos: Int): String {
    val minutos = segundos / 60
    val resto = segundos % 60
    return when {
        minutos == 0 -> "$resto s"
        resto == 0 -> "$minutos min"
        else -> "$minutos min $resto s"
    }
}

// Fecha del comentario, por ejemplo "26-10-2026 18:40"
private fun textoFecha(milisegundos: Long): String =
    SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("es", "CL")).format(Date(milisegundos))

// "a 35 m" bajo 1 km, "a 1,2 km" desde 1 km, "Calculando distancia…" sin ubicación
private fun textoDistancia(metros: Double?): String = when {
    metros == null -> "Calculando distancia…"
    metros < 1000 -> "a ${metros.roundToInt()} m"
    else -> "a " + String.format(Locale("es", "CL"), "%.1f", metros / 1000) + " km"
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DetallePreview() {
    TriskeleduTheme {
        DetalleContent(
            estado = DetalleUiState(punto = DatosPrueba.PUNTO_LA_MONEDA, distanciaMetros = 35.0, cargando = false),
            onEvent = {},
            alVolver = {},
            alAbrirConfiguracion = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DetalleNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        DetalleContent(
            estado = DetalleUiState(
                punto = DatosPrueba.PUNTO_LA_MONEDA,
                capsula = DatosPrueba.CAPSULA_IA_01,
                estadoPunto = EstadoPunto.DESBLOQUEADO,
                puntosLugar = 5,
                distanciaMetros = 35.0,
                cargando = false,
                formularioComentarioAbierto = true,
                textoComentario = "Me gustó el cambio de guardia",
                comentarios = listOf(
                    Comentario(usuarioId = "u1", puntoId = DatosPrueba.PUNTO_LA_MONEDA.id, texto = "Muy buena la explicación del video")
                ),
                usuarioId = "u1"
            ),
            onEvent = {},
            alVolver = {},
            alAbrirConfiguracion = {}
        )
    }
}
