package com.cookingplastic.triskeledu.ui.screens.admin

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.datosFaltantes
import com.cookingplastic.triskeledu.data.model.puedeActivarse
import com.cookingplastic.triskeledu.ui.event.AdminListaUiEvent
import com.cookingplastic.triskeledu.ui.state.AdminListaUiState
import com.cookingplastic.triskeledu.ui.theme.ColoresApp
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.AdminListaViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.unirConY
import java.util.Locale

// Pantalla 9 · Admin: puntos de interés. Conecta el ViewModel con el contenido.
@Composable
fun AdminListaScreen(
    alSalir: () -> Unit,
    alCrear: () -> Unit,
    alEditar: (String) -> Unit,
    viewModel: AdminListaViewModel = viewModel(factory = AdminListaViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    AdminListaContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        alSalir = alSalir,
        alCrear = alCrear,
        alEditar = alEditar
    )
}

@Composable
fun AdminListaContent(
    estado: AdminListaUiState,
    onEvent: (AdminListaUiEvent) -> Unit,
    alSalir: () -> Unit,
    alCrear: () -> Unit,
    alEditar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            onEvent(AdminListaUiEvent.OnMensajeMostrado)
        }
    }

    // A-10: confirmación antes de eliminar
    estado.puntoAEliminar?.let { punto ->
        AlertDialog(
            onDismissRequest = { onEvent(AdminListaUiEvent.OnCancelarEliminar) },
            title = { Text("¿Eliminar «${punto.nombre}»?") },
            text = { Text("El punto deja de existir y desaparece del mapa. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { onEvent(AdminListaUiEvent.OnConfirmarEliminar) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(AdminListaUiEvent.OnCancelarEliminar) }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CabeceraAdmin(alSalir = alSalir)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Título y totales (A-03)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Puntos de interés",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${estado.puntos.size} puntos · ${estado.totalActivos} activos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Búsqueda por nombre (A-04)
                item {
                    OutlinedTextField(
                        value = estado.busqueda,
                        onValueChange = { onEvent(AdminListaUiEvent.OnBusquedaChange(it)) },
                        placeholder = { Text("Buscar por nombre") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                when {
                    estado.cargando -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    estado.puntosFiltrados.isEmpty() -> item {
                        Text(
                            text = "No hay puntos con ese nombre",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                    else -> items(estado.puntosFiltrados, key = { it.id }) { punto ->
                        TarjetaPuntoAdmin(
                            punto = punto,
                            alCambiarActivo = { activo -> onEvent(AdminListaUiEvent.OnCambiarActivo(punto.id, activo)) },
                            alEditar = { alEditar(punto.id) },
                            alEliminar = { onEvent(AdminListaUiEvent.OnPedirEliminar(punto)) }
                        )
                    }
                }

                // A-13: recordatorio de la regla de activación
                item {
                    Text(
                        text = "Un punto se activa solo cuando tiene nombre, tipo, ubicación, imagen, video y QR. La descripción es opcional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = alCrear,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nuevo punto", style = MaterialTheme.typography.labelLarge) },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        )

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        )
    }
}

// A-12: cabecera con el color propio del modo administrador y el botón "Salir"
@Composable
private fun CabeceraAdmin(alSalir: () -> Unit) {
    val colorTexto = ColoresApp.sobreBarraAdmin
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColoresApp.barraAdmin)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = colorTexto)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Modo administrador",
            style = MaterialTheme.typography.titleSmall,
            color = colorTexto,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = alSalir,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, colorTexto),
            modifier = Modifier.height(48.dp)
        ) {
            Text("Salir", style = MaterialTheme.typography.labelMedium, color = colorTexto)
        }
    }
}

// A-05 y A-06: tarjeta de un punto con su franja de estado
@Composable
private fun TarjetaPuntoAdmin(
    punto: PuntoInteres,
    alCambiarActivo: (Boolean) -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit
) {
    val completo = punto.puedeActivarse()
    val bloqueado = !completo && !punto.activo

    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = punto.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${punto.tipo.descripcion} · " + String.format(
                            Locale.US, "%.4f, %.4f", punto.ubicacion.latitud, punto.ubicacion.longitud
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BotonCuadradoAdmin(alPresionar = alEditar) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar ${punto.nombre}")
                }
                BotonCuadradoAdmin(alPresionar = alEliminar) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar ${punto.nombre}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Franja inferior: activo, inactivo o "No se puede activar" con lo que falta
            val fondoFranja = when {
                bloqueado -> MaterialTheme.colorScheme.secondaryContainer
                punto.activo -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(fondoFranja)
                    .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (bloqueado) {
                        Text(
                            text = "No se puede activar",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Falta: ${unirConY(punto.datosFaltantes())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = if (punto.activo) "Activo · visible en el mapa" else "Inactivo · oculto temporalmente",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (punto.activo) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // A-09: la pista del interruptor cambia de color con una animación
                val colorPista by animateColorAsState(
                    targetValue = if (punto.activo) ColoresApp.interruptorEncendido else ColoresApp.bloqueado,
                    label = "pistaInterruptor"
                )
                Switch(
                    checked = punto.activo,
                    onCheckedChange = alCambiarActivo,
                    // A-07: sin datos completos no se puede activar
                    enabled = !bloqueado,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colorPista,
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        uncheckedTrackColor = colorPista,
                        uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                        uncheckedBorderColor = colorPista
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = if (bloqueado) {
                            "Activar ${punto.nombre} (faltan datos)"
                        } else {
                            "Activar ${punto.nombre}"
                        }
                    }
                )
            }
        }
    }
}

// Botón de 44 dp con esquinas de 12 dp y fondo de superficie variante (Editar y Eliminar)
@Composable
private fun BotonCuadradoAdmin(alPresionar: () -> Unit, contenido: @Composable () -> Unit) {
    IconButton(
        onClick = alPresionar,
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        contenido()
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AdminListaPreview() {
    TriskeleduTheme {
        AdminListaContent(
            estado = AdminListaUiState(puntos = DatosPrueba.puntos.sortedBy { it.nombre }, cargando = false),
            onEvent = {},
            alSalir = {},
            alCrear = {},
            alEditar = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AdminListaNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        AdminListaContent(
            estado = AdminListaUiState(puntos = DatosPrueba.puntos.sortedBy { it.nombre }, cargando = false),
            onEvent = {},
            alSalir = {},
            alCrear = {},
            alEditar = {}
        )
    }
}
