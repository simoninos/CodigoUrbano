package com.cookingplastic.triskeledu.ui.screens.mislugares

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.ui.components.BotonMapa
import com.cookingplastic.triskeledu.ui.components.EsferaAgua
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.event.MisLugaresUiEvent
import com.cookingplastic.triskeledu.ui.state.EstadoLugar
import com.cookingplastic.triskeledu.ui.state.FiltroLugares
import com.cookingplastic.triskeledu.ui.state.LugarDesbloqueado
import com.cookingplastic.triskeledu.ui.state.MisLugaresUiState
import com.cookingplastic.triskeledu.ui.state.OrdenLugares
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.MisLugaresViewModel

//Pantalla 7: se conecta al ViewModel y avisa cuando hay que navegar
@Composable
fun MisLugaresScreen(
    alVerDetalle: (String) -> Unit,
    alAbrirConfiguracion: () -> Unit,
    viewModel: MisLugaresViewModel = viewModel(factory = MisLugaresViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    MisLugaresContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        alVerDetalle = alVerDetalle,
        alAbrirConfiguracion = alAbrirConfiguracion
    )
}

//Solo dibuja; los Preview la muestran con datos de ejemplo
@Composable
fun MisLugaresContent(
    estado: MisLugaresUiState,
    onEvent: (MisLugaresUiEvent) -> Unit,
    alVerDetalle: (String) -> Unit,
    alAbrirConfiguracion: () -> Unit,
    modifier: Modifier = Modifier
) {
    //LazyColumn solo dibuja las filas que caben en pantalla: sirve aunque haya muchos lugares
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        //título con el engranaje de configuración, como en el mockup
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Mis lugares",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                BotonMapa(
                    icono = Icons.Filled.Settings,
                    descripcion = "Abrir configuración",
                    alPresionar = alAbrirConfiguracion
                )
            }
        }

        //las dos tarjetas de totales, una al lado de la otra
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaTotal(
                    titulo = "Mis puntos",
                    cifra = "${estado.totalPuntos}",
                    detalle = null,
                    fondo = MaterialTheme.colorScheme.secondary,
                    texto = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.weight(1f)
                )
                TarjetaTotal(
                    titulo = "Lugares escaneados",
                    cifra = "${estado.lugaresEscaneados}",
                    detalle = "de ${estado.totalLugares}",
                    fondo = MaterialTheme.colorScheme.primaryContainer,
                    texto = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { MensajeError(mensaje = estado.mensajeError) }

        //título de la lista con los menús de filtrar y ordenar
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Desbloqueados",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                MenuOpciones(
                    icono = Icons.Filled.FilterList,
                    descripcion = "Filtrar",
                    opciones = FiltroLugares.entries,
                    seleccionada = estado.filtro,
                    texto = { it.texto },
                    alElegir = { onEvent(MisLugaresUiEvent.OnFiltroChange(it)) }
                )
                MenuOpciones(
                    icono = Icons.AutoMirrored.Filled.Sort,
                    descripcion = "Ordenar",
                    opciones = OrdenLugares.entries,
                    seleccionada = estado.orden,
                    texto = { it.texto },
                    alElegir = { onEvent(MisLugaresUiEvent.OnOrdenChange(it)) }
                )
            }
        }

        if (estado.desbloqueados.isEmpty() && !estado.cargando) {
            item {
                Text(
                    text = if (estado.filtro == FiltroLugares.TODOS) "Todavía no escaneas ningún lugar. Búscalos en el mapa."
                    else "No hay lugares con este filtro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        //key ayuda a Compose a saber qué fila es cuál cuando la lista se reordena
        items(estado.desbloqueados, key = { it.puntoId }) { lugar ->
            TarjetaLugar(lugar = lugar, alPresionar = { alVerDetalle(lugar.puntoId) })
        }

        if (estado.bloqueados > 0) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Por descubrir",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${estado.bloqueados} ${if (estado.bloqueados == 1) "lugar" else "lugares"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            //los bloqueados no muestran su nombre, como pide el plan
            items(estado.bloqueados) {
                TarjetaBloqueada()
            }
        }
    }
}

@Composable
private fun TarjetaTotal(
    titulo: String,
    cifra: String,
    detalle: String?,
    fondo: Color,
    texto: Color,
    modifier: Modifier = Modifier
) {
    Surface(color = fondo, contentColor = texto, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = titulo, style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = cifra, style = MaterialTheme.typography.displaySmall)
                if (detalle != null) {
                    Text(
                        text = " $detalle",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}

//Un lugar desbloqueado: nombre, tipo, estado y su esfera de agua. Al tocarlo se abre su detalle.
//Los completados se destacan con fondo verde claro y borde verde
@Composable
private fun TarjetaLugar(lugar: LugarDesbloqueado, alPresionar: () -> Unit) {
    val completado = lugar.estado == EstadoLugar.COMPLETADO
    Surface(
        onClick = alPresionar,
        shape = RoundedCornerShape(16.dp),
        color = if (completado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (completado) 2.dp else 1.dp,
            color = if (completado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = lugar.nombre, style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = lugar.tipo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Text(
                        text = lugar.estado.texto,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (completado) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            EsferaAgua(puntos = lugar.puntos, maximo = lugar.maximo)
        }
    }
}

@Composable
private fun TarjetaBloqueada() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = "Bloqueado", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Encuéntralo en el mapa y escanea su QR",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

//Botón con ícono que abre una lista de opciones. <T> permite usarlo con cualquier tipo de opción:
//aquí sirve tanto para FiltroLugares como para OrdenLugares
@Composable
private fun <T> MenuOpciones(
    icono: ImageVector,
    descripcion: String,
    opciones: List<T>,
    seleccionada: T,
    texto: (T) -> String,
    alElegir: (T) -> Unit
) {
    //abierto vive solo en la pantalla: no es un dato de la app, solo si el menú se ve o no
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(imageVector = icono, contentDescription = descripcion)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(texto(opcion)) },
                    onClick = {
                        alElegir(opcion)
                        abierto = false
                    },
                    //un check junto a la opción elegida
                    leadingIcon = if (opcion == seleccionada) {
                        { Icon(imageVector = Icons.Filled.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MisLugaresPreview() {
    val ahora = System.currentTimeMillis()
    TriskeleduTheme {
        MisLugaresContent(
            estado = MisLugaresUiState(
                totalPuntos = 30,
                lugaresEscaneados = 3,
                totalLugares = 5,
                desbloqueados = listOf(
                    LugarDesbloqueado("p1", "La Moneda", "Monumento", 5, 20, EstadoLugar.RECIEN_DESCUBIERTO, ahora),
                    LugarDesbloqueado("p2", "Museo de Bellas Artes", "Monumento", 5, 20, EstadoLugar.FALTA_VIDEO, ahora - 1),
                    LugarDesbloqueado("p3", "Museo de Arte Precolombino", "Monumento", 20, 20, EstadoLugar.COMPLETADO, ahora - 2)
                ),
                bloqueados = 2,
                cargando = false
            ),
            onEvent = {},
            alVerDetalle = {},
            alAbrirConfiguracion = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MisLugaresVacioPreview() {
    TriskeleduTheme {
        MisLugaresContent(
            estado = MisLugaresUiState(totalLugares = 5, bloqueados = 5, cargando = false),
            onEvent = {},
            alVerDetalle = {},
            alAbrirConfiguracion = {}
        )
    }
}