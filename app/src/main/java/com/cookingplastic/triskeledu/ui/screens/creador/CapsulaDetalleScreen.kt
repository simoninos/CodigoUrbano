package com.cookingplastic.triskeledu.ui.screens.creador

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.event.CapsulaDetalleUiEvent
import com.cookingplastic.triskeledu.ui.state.CapsulaDetalleUiState
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.validation.EnlaceVideo
import com.cookingplastic.triskeledu.ui.validation.ReglaCapsula
import com.cookingplastic.triskeledu.ui.viewmodels.CapsulaDetalleViewModel

//P17-B · Detalle de la cápsula (CRE-05): se llega desde una cápsula publicada de P17
@Composable
fun CapsulaDetalleScreen(
    alVolver: () -> Unit,
    alEditar: (String) -> Unit,
    viewModel: CapsulaDetalleViewModel = viewModel(factory = CapsulaDetalleViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    //retirada: vuelve a ser borrador y se regresa a Mis cápsulas
    LaunchedEffect(estado.retirada) { if (estado.retirada) alVolver() }
    CapsulaDetalleContent(estado = estado, onEvent = viewModel::onEvent, alVolver = alVolver, alEditar = alEditar)
}

@Composable
fun CapsulaDetalleContent(
    estado: CapsulaDetalleUiState,
    onEvent: (CapsulaDetalleUiEvent) -> Unit,
    alVolver: () -> Unit,
    alEditar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val capsula = estado.capsula
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CabeceraCreador(titulo = "Detalle de la cápsula", alVolver = alVolver) {
            capsula?.let { EtiquetaEstadoCapsula(it.estado) }
        }
        when {
            estado.cargando -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            capsula == null -> Text("Esta cápsula ya no existe.", style = MaterialTheme.typography.bodyLarge)
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MiniaturaCapsula(ancho = 96.dp, alto = 128.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = capsula.titulo, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            EtiquetaCategoria(capsula.categoria.etiqueta)
                            EtiquetaCategoria(textoDuracion(capsula.duracionSegundos))
                        }
                        capsula.descripcion?.takeIf { it.isNotBlank() }?.let {
                            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                TarjetaPuntoAsociado(estado)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Enlace de Google Drive", style = MaterialTheme.typography.titleSmall)
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = textoEnlace(capsula.urlStream),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { alEditar(capsula.id) },
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar")
                }
                //RN-21: con el punto activo el botón queda deshabilitado y abajo dice por qué
                val bloqueado = estado.motivoNoRetirable != null
                OutlinedButton(
                    onClick = { onEvent(CapsulaDetalleUiEvent.OnRetirar) },
                    enabled = !bloqueado && !estado.retirando,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.error.copy(alpha = if (bloqueado) 0.38f else 1f)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text(if (estado.retirando) "Retirando..." else "Retirar publicación") }
                MensajeError(mensaje = estado.motivoNoRetirable ?: estado.mensajeError)
            }
        }
    }

    if (estado.confirmarRetiro && capsula != null) {
        AlertDialog(
            onDismissRequest = { onEvent(CapsulaDetalleUiEvent.OnCancelarRetiro) },
            title = { Text("¿Retirar la publicación?") },
            text = {
                Text(
                    estado.punto?.let { "“${capsula.titulo}” vuelve a borrador y se quita de ${it.nombre}, que queda Sin cápsula." }
                        ?: "“${capsula.titulo}” vuelve a borrador."
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(CapsulaDetalleUiEvent.OnConfirmarRetiro) }) {
                    Text("Retirar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { onEvent(CapsulaDetalleUiEvent.OnCancelarRetiro) }) { Text("Cancelar") } }
        )
    }
}

//"Asociada a": el punto con su estado y las vistas completas, o la invitación a asociarla desde el mapa
@Composable
private fun TarjetaPuntoAsociado(estado: CapsulaDetalleUiState) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "ASOCIADA A", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val punto = estado.punto
            if (punto == null) {
                Text(
                    text = "Sin punto: asígnala desde el mapa a un punto Sin cápsula.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(text = punto.nombre, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
                    estado.estadoPunto?.let { EtiquetaEstadoPunto(it) }
                }
            }
            Text(
                text = "Vistas completas: ${estado.vistasCompletas}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

//RN-35 y RN-36: las cápsulas de los datos de prueba no tienen enlace de Drive
private fun textoEnlace(enlace: String?): String = when {
    enlace.isNullOrBlank() -> "Sin enlace (cápsula de los datos de prueba)"
    !EnlaceVideo.necesitaInternet(enlace) -> "Clip de prueba incluido en la app"
    else -> enlace
}

@Composable
fun EtiquetaEstadoPunto(estado: EstadoAdminPunto) {
    when (estado) {
        EstadoAdminPunto.ACTIVO -> Etiqueta(estado.etiqueta, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        EstadoAdminPunto.INACTIVO -> Etiqueta(estado.etiqueta, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurfaceVariant)
        EstadoAdminPunto.SIN_CAPSULA -> Etiqueta(estado.etiqueta, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Preview(name = "Detalle con punto activo", showBackground = true)
@Composable
private fun CapsulaDetallePreview() {
    TriskeleduTheme {
        Surface {
            CapsulaDetalleContent(
                estado = CapsulaDetalleUiState(
                    capsula = DatosPrueba.CAPSULA_LA_MONEDA,
                    punto = DatosPrueba.PUNTO_LA_MONEDA,
                    estadoPunto = EstadoAdminPunto.ACTIVO,
                    vistasCompletas = 31,
                    motivoNoRetirable = ReglaCapsula.RETIRO_BLOQUEADO,
                    cargando = false
                ),
                onEvent = {}, alVolver = {}, alEditar = {}
            )
        }
    }
}
