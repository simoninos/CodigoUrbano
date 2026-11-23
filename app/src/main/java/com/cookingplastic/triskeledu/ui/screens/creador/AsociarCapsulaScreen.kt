package com.cookingplastic.triskeledu.ui.screens.creador

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.event.AsociarCapsulaUiEvent
import com.cookingplastic.triskeledu.ui.state.AsociarCapsulaUiState
import com.cookingplastic.triskeledu.ui.state.OpcionCapsula
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.AsociarCapsulaViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.armarAsociar

//P21 · Asociar cápsula (CRE-08): desde la tarjeta de un punto Sin cápsula en el mapa del creador
@Composable
fun AsociarCapsulaScreen(
    alVolver: () -> Unit,
    viewModel: AsociarCapsulaViewModel = viewModel(factory = AsociarCapsulaViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    //asociada: el punto quedó Inactivo y se vuelve al mapa
    LaunchedEffect(estado.asociada) { if (estado.asociada) alVolver() }
    AsociarCapsulaContent(estado = estado, onEvent = viewModel::onEvent, alVolver = alVolver)
}

@Composable
fun AsociarCapsulaContent(
    estado: AsociarCapsulaUiState,
    onEvent: (AsociarCapsulaUiEvent) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).selectableGroup(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CabeceraCreador(titulo = "Asociar cápsula", alVolver = alVolver) }
            estado.punto?.let { punto ->
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PUNTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(punto.nombre, style = MaterialTheme.typography.titleSmall)
                            }
                            EtiquetaEstadoPunto(EstadoAdminPunto.SIN_CAPSULA)
                        }
                    }
                }
            }
            item {
                Text("Tus cápsulas", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Solo puedes asignar cápsulas tuyas, publicadas y que no estén en otro punto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!estado.cargando && estado.opciones.isEmpty()) {
                item {
                    Text(
                        "Todavía no tienes cápsulas. Créalas desde Configuración › Mis cápsulas.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(estado.opciones, key = { it.capsula.id }) { opcion ->
                FilaOpcion(
                    opcion = opcion,
                    elegida = opcion.capsula.id == estado.elegidaId,
                    alElegir = { onEvent(AsociarCapsulaUiEvent.OnElegir(opcion.capsula.id)) }
                )
            }
        }
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            //RN-28: activarlo sigue siendo tarea del administrador
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Al asignarla, el punto queda Inactivo hasta que un administrador lo active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            MensajeError(mensaje = estado.motivoPunto ?: estado.mensajeError)
            Button(
                onClick = { onEvent(AsociarCapsulaUiEvent.OnAsociar) },
                enabled = estado.puedeAsociar,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(
                    text = if (estado.asociando) "Asociando..." else "Asociar a ${estado.punto?.nombre ?: "este punto"}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

//Una cápsula con su radio; si no se puede elegir, se ve atenuada con el motivo (CRE-08)
@Composable
private fun FilaOpcion(opcion: OpcionCapsula, elegida: Boolean, alElegir: () -> Unit) {
    val capsula = opcion.capsula
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (elegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (elegida) 2.dp else 1.dp, if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = elegida, enabled = opcion.elegible, role = Role.RadioButton, onClick = alElegir)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).alpha(if (opcion.elegible) 1f else 0.6f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(selected = elegida, onClick = null, enabled = opcion.elegible)
            MiniaturaCapsula(ancho = 40.dp, alto = 52.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(capsula.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EtiquetaCategoria(capsula.categoria.etiqueta)
                    EtiquetaEstadoCapsula(capsula.estado)
                }
                Text(
                    text = opcion.motivo ?: "Libre: se puede asignar",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (opcion.elegible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(name = "Asociar cápsula", showBackground = true)
@Composable
private fun AsociarCapsulaPreview() {
    TriskeleduTheme {
        Surface {
            AsociarCapsulaContent(
                estado = armarAsociar(DatosPrueba.PUNTO_PALACIO_MATTE.id, DatosPrueba.USUARIO_CONTENIDO, DatosPrueba.capsulas.takeLast(3), DatosPrueba.puntos)
                    .copy(elegidaId = DatosPrueba.CAPSULA_LIBRE.id),
                onEvent = {},
                alVolver = {}
            )
        }
    }
}
