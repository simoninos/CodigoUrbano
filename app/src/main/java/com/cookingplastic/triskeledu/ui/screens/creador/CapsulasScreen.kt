package com.cookingplastic.triskeledu.ui.screens.creador

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.CategoriaCapsula
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.components.CabeceraCreador
import com.cookingplastic.triskeledu.ui.components.EtiquetaCategoria
import com.cookingplastic.triskeledu.ui.components.EtiquetaEstadoCapsula
import com.cookingplastic.triskeledu.ui.components.MiniaturaCapsula

//P17 · Mis cápsulas (CRE-04): se llega desde la tarjeta "Mis cápsulas" de Configuración
@Composable
fun CapsulasScreen(
    alVolver: () -> Unit,
    alNuevaCapsula: () -> Unit,
    alEditar: (String) -> Unit,
    alVerDetalle: (String) -> Unit,
    viewModel: CapsulasViewModel = viewModel(factory = CapsulasViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    CapsulasContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        alVolver = alVolver,
        alNuevaCapsula = alNuevaCapsula,
        //un borrador se abre en el formulario; una publicada, en su detalle (P17-B)
        alAbrir = { fila -> if (fila.capsula.estado == EstadoCapsula.BORRADOR) alEditar(fila.capsula.id) else alVerDetalle(fila.capsula.id) }
    )
}

@Composable
fun CapsulasContent(
    estado: CapsulasUiState,
    onEvent: (CapsulasUiEvent) -> Unit,
    alVolver: () -> Unit,
    alNuevaCapsula: () -> Unit,
    alAbrir: (FilaCapsula) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CabeceraCreador(titulo = "Mis cápsulas", alVolver = alVolver) }
            item {
                OutlinedTextField(
                    value = estado.busqueda,
                    onValueChange = { onEvent(CapsulasUiEvent.OnBusquedaChange(it)) },
                    placeholder = { Text("Buscar por título") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { Filtros(estado = estado, onEvent = onEvent) }
            if (!estado.cargando && estado.capsulas.isEmpty()) {
                item {
                    Text(
                        text = if (estado.totalPropias == 0) "Todavía no tienes cápsulas. Crea la primera con “Nueva cápsula”."
                        else "No hay cápsulas con estos filtros.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }
            items(estado.capsulas, key = { it.capsula.id }) { fila -> TarjetaCapsula(fila = fila, alPresionar = { alAbrir(fila) }) }
        }
        ExtendedFloatingActionButton(
            onClick = alNuevaCapsula,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nueva cápsula") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

//Todas / Borradores / Publicadas y el menú de categoría; se desplaza de lado si no cabe
@Composable
private fun Filtros(estado: CapsulasUiState, onEvent: (CapsulasUiEvent) -> Unit) {
    var menuAbierto by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FiltroEstadoCapsula.entries.forEach { filtro ->
            FilterChip(
                selected = estado.filtroEstado == filtro,
                onClick = { onEvent(CapsulasUiEvent.OnFiltroEstadoChange(filtro)) },
                label = { Text(filtro.texto) }
            )
        }
        Box {
            FilterChip(
                selected = estado.categoria != null,
                onClick = { menuAbierto = true },
                label = { Text(estado.categoria?.etiqueta ?: "Categoría") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
            )
            DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                DropdownMenuItem(text = { Text("Todas las categorías") }, onClick = {
                    onEvent(CapsulasUiEvent.OnCategoriaChange(null))
                    menuAbierto = false
                })
                CategoriaCapsula.entries.forEach { categoria ->
                    DropdownMenuItem(text = { Text(categoria.etiqueta) }, onClick = {
                        onEvent(CapsulasUiEvent.OnCategoriaChange(categoria))
                        menuAbierto = false
                    })
                }
            }
        }
    }
}

//Una cápsula: miniatura, título, categoría, estado y el punto (CRE-04: si ya está asociada)
@Composable
private fun TarjetaCapsula(fila: FilaCapsula, alPresionar: () -> Unit) {
    val capsula = fila.capsula
    Surface(
        onClick = alPresionar,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniaturaCapsula()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = capsula.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EtiquetaCategoria(capsula.categoria.etiqueta)
                    EtiquetaEstadoCapsula(capsula.estado)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(
                        text = when {
                            fila.nombrePunto != null -> fila.nombrePunto
                            capsula.estado == EstadoCapsula.PUBLICADA -> "Sin punto: asígnala desde el mapa"
                            else -> "Sin punto"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "Mis cápsulas", showBackground = true)
@Composable
private fun CapsulasPreview() {
    TriskeleduTheme {
        Surface {
            CapsulasContent(
                estado = armarCapsulas(
                    DatosPrueba.capsulas.filter { it.creadorId == DatosPrueba.USUARIO_CONTENIDO.id }.takeLast(4),
                    DatosPrueba.puntos, "", FiltroEstadoCapsula.TODAS, null
                ),
                onEvent = {}, alVolver = {}, alNuevaCapsula = {}, alAbrir = {}
            )
        }
    }
}
