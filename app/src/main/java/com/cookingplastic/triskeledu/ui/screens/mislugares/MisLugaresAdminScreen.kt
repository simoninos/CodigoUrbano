package com.cookingplastic.triskeledu.ui.screens.mislugares

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.ui.components.BotonCampana
import com.cookingplastic.triskeledu.ui.components.BotonMapa
import com.cookingplastic.triskeledu.ui.event.AdminListaUiEvent
import com.cookingplastic.triskeledu.ui.screens.creador.Etiqueta
import com.cookingplastic.triskeledu.ui.screens.creador.EtiquetaEstadoPunto
import com.cookingplastic.triskeledu.ui.state.AdminListaUiState
import com.cookingplastic.triskeledu.ui.theme.ColoresApp
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.validation.ReglaPunto
import com.cookingplastic.triskeledu.ui.viewmodels.AdminListaViewModel

//P07-Admin · Mis lugares del administrador (LUG-05, ADM-01): es la pestaña Mis lugares con rol
//ADMINISTRADOR y reemplaza a la antigua pantalla 9. La lógica sigue en AdminListaViewModel
@Composable
fun MisLugaresAdminScreen(
    alNuevoPunto: () -> Unit,
    alEditar: (String) -> Unit,
    alAbrirConfiguracion: () -> Unit,
    alAbrirNotificaciones: () -> Unit,
    notificacionesSinLeer: Int,
    viewModel: AdminListaViewModel = viewModel(factory = AdminListaViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    MisLugaresAdminContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        alNuevoPunto = alNuevoPunto,
        alEditar = alEditar,
        alAbrirConfiguracion = alAbrirConfiguracion,
        alAbrirNotificaciones = alAbrirNotificaciones,
        notificacionesSinLeer = notificacionesSinLeer
    )
}

@Composable
fun MisLugaresAdminContent(
    estado: AdminListaUiState,
    onEvent: (AdminListaUiEvent) -> Unit,
    alNuevoPunto: () -> Unit,
    alEditar: (String) -> Unit,
    alAbrirConfiguracion: () -> Unit,
    alAbrirNotificaciones: () -> Unit,
    notificacionesSinLeer: Int,
    modifier: Modifier = Modifier
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            onEvent(AdminListaUiEvent.OnMensajeMostrado)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Mis lugares",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f).semantics { heading() }
                    )
                    //PER-03: la campana abre el historial; ahí está el botón "Solicitudes de creador" (ADM-10)
                    BotonCampana(sinLeer = notificacionesSinLeer, alPresionar = alAbrirNotificaciones)
                    BotonMapa(icono = Icons.Filled.Settings, descripcion = "Abrir configuración", alPresionar = alAbrirConfiguracion)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = estado.busqueda,
                        onValueChange = { onEvent(AdminListaUiEvent.OnBusquedaChange(it)) },
                        placeholder = { Text("Buscar punto") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(onClick = alNuevoPunto, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "Nuevo punto")
                    }
                }
            }
            item { FiltrosEstado(estado = estado, onEvent = onEvent) }
            if (!estado.cargando && estado.puntosFiltrados.isEmpty()) {
                item {
                    Text(
                        text = if (estado.puntos.isEmpty()) "Todavía no hay puntos. Crea el primero con +." else "No hay puntos con este filtro.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }
            items(estado.puntosFiltrados, key = { it.id }) { punto ->
                TarjetaPuntoAdmin(
                    punto = punto,
                    estadoPunto = estado.estadoDe(punto),
                    escaneos = estado.escaneosDe(punto),
                    onEvent = onEvent,
                    alEditar = { alEditar(punto.id) }
                )
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    //A-10: eliminar pide confirmación
    estado.puntoAEliminar?.let { punto ->
        AlertDialog(
            onDismissRequest = { onEvent(AdminListaUiEvent.OnCancelarEliminar) },
            title = { Text("¿Eliminar ${punto.nombre}?") },
            text = { Text("Se borra el punto y su QR deja de funcionar. Su cápsula queda libre para otro punto.") },
            confirmButton = {
                TextButton(onClick = { onEvent(AdminListaUiEvent.OnConfirmarEliminar) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { onEvent(AdminListaUiEvent.OnCancelarEliminar) }) { Text("Cancelar") } }
        )
    }
}

//Todos / Activos / Inactivos / Sin cápsula, con cuántos hay de cada uno
@Composable
private fun FiltrosEstado(estado: AdminListaUiState, onEvent: (AdminListaUiEvent) -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = estado.filtro == null,
            onClick = { onEvent(AdminListaUiEvent.OnFiltroChange(null)) },
            label = { Text("Todos") }
        )
        listOf(
            EstadoAdminPunto.ACTIVO to "Activos",
            EstadoAdminPunto.INACTIVO to "Inactivos",
            EstadoAdminPunto.SIN_CAPSULA to "Sin cápsula"
        ).forEach { (filtro, texto) ->
            FilterChip(
                selected = estado.filtro == filtro,
                onClick = { onEvent(AdminListaUiEvent.OnFiltroChange(filtro)) },
                label = { Text("$texto ${estado.cantidad(filtro)}") }
            )
        }
    }
}

//Una tarjeta: nombre, tipo y estado, el interruptor Activo, "Editar" (o "Asociar cápsula") y eliminar
@Composable
private fun TarjetaPuntoAdmin(
    punto: PuntoInteres,
    estadoPunto: EstadoAdminPunto,
    escaneos: Int,
    onEvent: (AdminListaUiEvent) -> Unit,
    alEditar: () -> Unit
) {
    val bloqueado = ReglaPunto.interruptorBloqueado(estadoPunto)
    val conProgreso = ReglaPunto.motivoNoEliminable(escaneos) != null
    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = punto.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    //el estado va primero; si el tipo es largo, es el tipo el que se acorta
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EtiquetaEstadoPunto(estadoPunto)
                        Etiqueta(
                            punto.tipo.descripcion,
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    when (estadoPunto) {
                        EstadoAdminPunto.ACTIVO -> Text(
                            text = "Visible en el mapa · ${if (escaneos == 1) "1 escaneo" else "$escaneos escaneos"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        EstadoAdminPunto.INACTIVO -> Text(
                            text = "Completo, oculto del mapa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        EstadoAdminPunto.SIN_CAPSULA -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = ReglaPunto.ACTIVAR_SIN_CAPSULA, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                InterruptorActivo(punto = punto, estadoPunto = estadoPunto, bloqueado = bloqueado, onEvent = onEvent)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                //Sin cápsula: la cápsula se elige al editar el punto (ADM-07)
                OutlinedButton(
                    onClick = alEditar,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(if (bloqueado) Icons.Outlined.Videocam else Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (bloqueado) "Asociar cápsula" else "Editar")
                }
                //RN-30: con progreso se ve bloqueado; al tocarlo explica por qué (RN-18)
                OutlinedIconButton(
                    onClick = { onEvent(AdminListaUiEvent.OnPedirEliminar(punto)) },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = if (conProgreso) 0.38f else 1f)),
                    colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .alpha(if (conProgreso) 0.5f else 1f)
                        .semantics {
                            contentDescription = if (conProgreso) "Eliminar ${punto.nombre}, bloqueado: ${ReglaPunto.ELIMINAR_CON_PROGRESO}"
                            else "Eliminar ${punto.nombre}"
                        }
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                }
            }
        }
    }
}

//ADM-09: Inactivo <-> Activo. Con Sin cápsula queda bloqueado y dice "Bloqueado"
@Composable
private fun InterruptorActivo(
    punto: PuntoInteres,
    estadoPunto: EstadoAdminPunto,
    bloqueado: Boolean,
    onEvent: (AdminListaUiEvent) -> Unit
) {
    val texto = when (estadoPunto) {
        EstadoAdminPunto.ACTIVO -> "Activo"
        EstadoAdminPunto.INACTIVO -> "Inactivo"
        EstadoAdminPunto.SIN_CAPSULA -> "Bloqueado"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        //A-09: la pista del interruptor cambia de color con una animación
        val colorPista by animateColorAsState(
            targetValue = if (punto.activo) ColoresApp.interruptorEncendido else ColoresApp.bloqueado,
            label = "pistaInterruptor"
        )
        Switch(
            checked = punto.activo,
            onCheckedChange = { onEvent(AdminListaUiEvent.OnCambiarActivo(punto.id, it)) },
            enabled = !bloqueado,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colorPista,
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = colorPista,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedBorderColor = colorPista
            ),
            modifier = Modifier.semantics {
                contentDescription = "Activar ${punto.nombre}"
                stateDescription = if (bloqueado) "Bloqueado: ${ReglaPunto.ACTIVAR_SIN_CAPSULA}" else texto
            }
        )
        Text(text = texto, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "Mis lugares del admin", showBackground = true)
@Composable
private fun MisLugaresAdminPreview() {
    TriskeleduTheme {
        Surface {
            MisLugaresAdminContent(
                estado = AdminListaUiState(
                    puntos = DatosPrueba.puntos.sortedBy { it.nombre }.take(6) + DatosPrueba.PUNTO_PALACIO_MATTE,
                    capsulas = DatosPrueba.capsulas.associateBy { it.id },
                    puntosConProgreso = mapOf(DatosPrueba.PUNTO_LA_MONEDA.id to 42),
                    cargando = false
                ),
                onEvent = {}, alNuevoPunto = {}, alEditar = {}, alAbrirConfiguracion = {}, alAbrirNotificaciones = {},
                notificacionesSinLeer = 1
            )
        }
    }
}
