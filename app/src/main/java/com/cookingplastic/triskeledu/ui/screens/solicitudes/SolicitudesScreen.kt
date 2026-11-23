package com.cookingplastic.triskeledu.ui.screens.solicitudes

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.ui.components.fechaConAnio
import com.cookingplastic.triskeledu.ui.event.SolicitudesUiEvent
import com.cookingplastic.triskeledu.ui.screens.creador.CabeceraCreador
import com.cookingplastic.triskeledu.ui.state.SolicitudesUiState
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.SolicitudesViewModel

//P22 · Solicitudes de creador (ADM-10). Mientras no exista P16 (etapa 8), se llega desde la campana del mapa
@Composable
fun SolicitudesScreen(
    alVolver: () -> Unit,
    viewModel: SolicitudesViewModel = viewModel(factory = SolicitudesViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    SolicitudesContent(estado = estado, onEvent = viewModel::onEvent, alVolver = alVolver)
}

@Composable
fun SolicitudesContent(
    estado: SolicitudesUiState,
    onEvent: (SolicitudesUiEvent) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            onEvent(SolicitudesUiEvent.OnMensajeVisto)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { CabeceraCreador(titulo = "Solicitudes de creador", alVolver = alVolver) }
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = buildAnnotatedString {
                            append("Al aprobar, la cuenta pasa a ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Creador") }
                            append(". En ambos casos se le avisa al usuario.")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            if (!estado.cargando) {
                item {
                    Text(
                        text = when (estado.pendientes.size) {
                            0 -> "No hay solicitudes pendientes."
                            1 -> "1 pendiente"
                            else -> "${estado.pendientes.size} pendientes"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(estado.pendientes, key = { it.id }) { usuario ->
                TarjetaSolicitud(usuario = usuario, habilitada = !estado.procesando, onEvent = onEvent)
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    estado.porConfirmar?.let { decision ->
        val nombre = decision.usuario.username
        AlertDialog(
            onDismissRequest = { onEvent(SolicitudesUiEvent.OnCancelar) },
            title = { Text(if (decision.aprobar) "¿Aprobar a $nombre como creador de contenido?" else "¿Rechazar la solicitud de $nombre?") },
            text = {
                Text(
                    if (decision.aprobar) "Su cuenta se desbloquea con el rol Creador y podrá crear y asignar cápsulas. Le llegará una notificación."
                    else "Su cuenta se desbloquea como cliente. Le llegará una notificación."
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(SolicitudesUiEvent.OnConfirmar) }) {
                    Text(
                        if (decision.aprobar) "Aprobar" else "Rechazar",
                        color = if (decision.aprobar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = { TextButton(onClick = { onEvent(SolicitudesUiEvent.OnCancelar) }) { Text("Cancelar") } }
        )
    }
}

//Una solicitud: iniciales, nombre de usuario, correo, fecha y los dos botones
@Composable
private fun TarjetaSolicitud(usuario: Usuario, habilitada: Boolean, onEvent: (SolicitudesUiEvent) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iniciales(usuario.username), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(usuario.username, style = MaterialTheme.typography.titleSmall)
                    Text(usuario.correo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    usuario.solicitadoEn?.let {
                        Text("Pidió el ${fechaConAnio(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onEvent(SolicitudesUiEvent.OnRechazar(usuario)) },
                    enabled = habilitada,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rechazar")
                }
                Button(
                    onClick = { onEvent(SolicitudesUiEvent.OnAprobar(usuario)) },
                    enabled = habilitada,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aprobar")
                }
            }
        }
    }
}

//"vale.rojas" -> "VR"; "fer_diaz" -> "FD"; una sola palabra -> sus dos primeras letras
private fun iniciales(nombre: String): String {
    val partes = nombre.split('.', '_', '-', ' ').filter { it.isNotBlank() }
    return if (partes.size >= 2) "${partes[0].first()}${partes[1].first()}".uppercase()
    else nombre.take(2).uppercase()
}

@Preview(name = "Solicitudes", showBackground = true)
@Composable
private fun SolicitudesPreview() {
    TriskeleduTheme {
        Surface {
            SolicitudesContent(
                estado = SolicitudesUiState(pendientes = listOf(DatosPrueba.USUARIO_PENDIENTE), cargando = false),
                onEvent = {},
                alVolver = {}
            )
        }
    }
}
