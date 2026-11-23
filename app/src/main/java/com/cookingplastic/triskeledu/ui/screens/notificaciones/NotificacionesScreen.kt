package com.cookingplastic.triskeledu.ui.screens.notificaciones

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.ui.event.NotificacionesUiEvent
import com.cookingplastic.triskeledu.ui.state.DestinoNotificacion
import com.cookingplastic.triskeledu.ui.state.FilaNotificacion
import com.cookingplastic.triskeledu.ui.state.NotificacionesUiState
import com.cookingplastic.triskeledu.ui.theme.ColoresApp
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.NotificacionesViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.armarNotificaciones

//P16 · Historial de notificaciones (PER-03): desde la campana. Tocar una la marca leída y abre lo relacionado.
//El administrador tiene además el botón "Solicitudes de creador", que abre P22
@Composable
fun NotificacionesScreen(
    alVolver: () -> Unit,
    alAbrirDestino: (DestinoNotificacion) -> Unit,
    viewModel: NotificacionesViewModel = viewModel(factory = NotificacionesViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.destino) {
        estado.destino?.let {
            viewModel.onEvent(NotificacionesUiEvent.OnDestinoAbierto)
            alAbrirDestino(it)
        }
    }
    NotificacionesContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        alVolver = alVolver,
        alAbrirSolicitudes = { alAbrirDestino(DestinoNotificacion.Solicitudes) }
    )
}

@Composable
fun NotificacionesContent(
    estado: NotificacionesUiState,
    onEvent: (NotificacionesUiEvent) -> Unit,
    alVolver: () -> Unit,
    alAbrirSolicitudes: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                Text("Notificaciones", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            }
        }
        //ADM-10: en verde, con el contador de solicitudes pendientes
        if (estado.esAdministrador) {
            item {
                Button(onClick = alAbrirSolicitudes, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Solicitudes de creador")
                    if (estado.solicitudesPendientes > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge { Text("${estado.solicitudesPendientes}") }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (estado.sinLeer) {
                        0 -> "Todo leído"
                        1 -> "1 sin leer"
                        else -> "${estado.sinLeer} sin leer"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (estado.sinLeer > 0) {
                    TextButton(onClick = { onEvent(NotificacionesUiEvent.OnMarcarTodasLeidas) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Marcar todas como leídas")
                    }
                }
            }
        }
        if (!estado.cargando && estado.filas.isEmpty()) {
            item {
                Text(
                    "Todavía no tienes notificaciones. Aquí verás los lugares cercanos, las cápsulas nuevas y tus logros.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }
        estado.filas.groupBy { it.grupo }.forEach { (grupo, filas) ->
            item(key = grupo.name) {
                Text(grupo.titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            items(filas, key = { it.notificacion.id }) { fila ->
                TarjetaNotificacion(fila = fila, alTocar = { onEvent(NotificacionesUiEvent.OnTocar(fila.notificacion)) })
            }
        }
    }
}

//Ícono por tipo, título, texto y fecha. Las no leídas tienen fondo de color y un punto (PER-03)
@Composable
private fun TarjetaNotificacion(fila: FilaNotificacion, alTocar: () -> Unit) {
    val notificacion = fila.notificacion
    val noLeida = !notificacion.leida
    val (icono, fondoIcono) = iconoDe(notificacion.tipo)
    Surface(
        onClick = alTocar,
        shape = RoundedCornerShape(18.dp),
        color = if (noLeida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (noLeida) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().semantics { stateDescription = if (noLeida) "Sin leer" else "Leída" }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(40.dp).background(fondoIcono, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(notificacion.titulo, style = MaterialTheme.typography.titleSmall)
                Text(notificacion.texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fila.fecha, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (noLeida) {
                Box(modifier = Modifier.padding(start = 8.dp, top = 4.dp).size(10.dp).background(MaterialTheme.colorScheme.error, CircleShape))
            }
        }
    }
}

@Composable
private fun iconoDe(tipo: TipoNotificacion): Pair<ImageVector, Color> = when (tipo) {
    TipoNotificacion.LUGAR_CERCANO -> Icons.Outlined.Place to MaterialTheme.colorScheme.surfaceVariant
    TipoNotificacion.NUEVA_CAPSULA -> Icons.Outlined.Videocam to MaterialTheme.colorScheme.primaryContainer
    TipoNotificacion.LOGRO -> Icons.Outlined.EmojiEvents to ColoresApp.puntos.copy(alpha = 0.35f)
    TipoNotificacion.SOLICITUD -> Icons.Outlined.Person to MaterialTheme.colorScheme.surfaceVariant
    TipoNotificacion.PUNTO_LISTO -> Icons.Outlined.TaskAlt to MaterialTheme.colorScheme.secondaryContainer
    TipoNotificacion.SISTEMA -> Icons.Outlined.Info to MaterialTheme.colorScheme.surfaceVariant
}

@Preview(name = "Notificaciones", showBackground = true)
@Composable
private fun NotificacionesPreview() {
    val ahora = 1_800_000_000_000L
    val lista = listOf(
        Notificacion(usuarioId = "u", tipo = TipoNotificacion.LUGAR_CERCANO, titulo = "Lugar cercano", texto = "Estás a 150 m de Plaza de Armas. ¡Búscalo!", creadaEn = ahora - 60_000),
        Notificacion(usuarioId = "u", tipo = TipoNotificacion.LOGRO, titulo = "¡Lugar completado!", texto = "Terminaste Museo de Arte Precolombino: 80 de 80 puntos.", creadaEn = ahora - 3_600_000),
        Notificacion(usuarioId = "u", tipo = TipoNotificacion.NUEVA_CAPSULA, titulo = "Nueva cápsula disponible", texto = "Estación Mapocho ya tiene su cápsula sobre IA.", creadaEn = ahora - 3 * 86_400_000L, leida = true)
    )
    TriskeleduTheme {
        Surface {
            NotificacionesContent(
                estado = NotificacionesUiState(filas = armarNotificaciones(lista, ahora), sinLeer = 2, cargando = false),
                onEvent = {}, alVolver = {}, alAbrirSolicitudes = {}
            )
        }
    }
}
