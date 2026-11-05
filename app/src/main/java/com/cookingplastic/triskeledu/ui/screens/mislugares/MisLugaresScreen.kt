package com.cookingplastic.triskeledu.ui.screens.mislugares

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.ui.components.BotonCampana
import com.cookingplastic.triskeledu.ui.components.BotonMapa
import com.cookingplastic.triskeledu.ui.components.EsferaAgua
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.components.rememberReducirMovimiento
import com.cookingplastic.triskeledu.ui.event.MisLugaresUiEvent
import com.cookingplastic.triskeledu.ui.state.CapsulaDescubierta
import com.cookingplastic.triskeledu.ui.state.EstadoLugar
import com.cookingplastic.triskeledu.ui.state.FiltroLugares
import com.cookingplastic.triskeledu.ui.state.LugarDesbloqueado
import com.cookingplastic.triskeledu.ui.state.LugarPorDescubrir
import com.cookingplastic.triskeledu.ui.state.MisLugaresUiState
import com.cookingplastic.triskeledu.ui.state.OrdenLugares
import com.cookingplastic.triskeledu.ui.state.QrEscaneado
import com.cookingplastic.triskeledu.ui.state.VistaDescubrimientos
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.MisLugaresViewModel

//Pantalla 7 (P07-A a P07-D, Mis descubrimientos): se conecta al ViewModel y avisa cuando hay que navegar
@Composable
fun MisLugaresScreen(
    alVerDetalle: (String) -> Unit,
    alAbrirConfiguracion: () -> Unit,
    //P12: la cápsula se abre con su punto, y el reproductor elige el modo solo (RN-08)
    alVerCapsula: (capsulaId: String, puntoId: String) -> Unit,
    alIrAlMapa: () -> Unit,
    //P20 (LUG-06); si es null, "Ver ranking" no aparece
    alVerRanking: (() -> Unit)? = null,
    //PER-03: la campana con el contador de no leídas
    alAbrirNotificaciones: () -> Unit = {},
    notificacionesSinLeer: Int = 0,
    viewModel: MisLugaresViewModel = viewModel(factory = MisLugaresViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    //"Por descubrir" ya dejó el pedido del lugar: se abre el mapa una sola vez
    LaunchedEffect(estado.irAlMapa) {
        if (estado.irAlMapa) {
            viewModel.onEvent(MisLugaresUiEvent.OnMapaAbierto)
            alIrAlMapa()
        }
    }

    MisLugaresContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        alVerDetalle = alVerDetalle,
        alAbrirConfiguracion = alAbrirConfiguracion,
        alVerCapsula = alVerCapsula,
        alIrAlMapa = alIrAlMapa,
        alVerRanking = alVerRanking,
        alAbrirNotificaciones = alAbrirNotificaciones,
        notificacionesSinLeer = notificacionesSinLeer
    )
}

//Solo dibuja; los Preview la muestran con datos de ejemplo
@Composable
fun MisLugaresContent(
    estado: MisLugaresUiState,
    onEvent: (MisLugaresUiEvent) -> Unit,
    alVerDetalle: (String) -> Unit,
    alAbrirConfiguracion: () -> Unit,
    modifier: Modifier = Modifier,
    alVerCapsula: (capsulaId: String, puntoId: String) -> Unit = { _, _ -> },
    alIrAlMapa: () -> Unit = {},
    alVerRanking: (() -> Unit)? = null,
    alAbrirNotificaciones: () -> Unit = {},
    notificacionesSinLeer: Int = 0
) {
    val reducirMovimiento = rememberReducirMovimiento()

    //toda la pantalla se desplaza; la lista de la vista va en un solo elemento para animar el cambio de vista
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        //título con el engranaje de configuración, como en el mockup
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Mis descubrimientos",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                BotonCampana(sinLeer = notificacionesSinLeer, alPresionar = alAbrirNotificaciones)
                Spacer(modifier = Modifier.width(8.dp))
                BotonMapa(
                    icono = Icons.Filled.Settings,
                    descripcion = "Abrir configuración",
                    alPresionar = alAbrirConfiguracion
                )
            }
        }

        //LUG-01: la parte de arriba es igual en las cuatro vistas
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaTotal(
                    titulo = "Mis puntos",
                    cifra = "${estado.totalPuntos}",
                    detalle = null,
                    fondo = MaterialTheme.colorScheme.secondary,
                    texto = MaterialTheme.colorScheme.onSecondary,
                    alPresionar = alVerRanking,
                    enlace = if (alVerRanking != null) "Ver ranking ›" else null,
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
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CuadroVista(Icons.Outlined.QrCodeScanner, "QR", estado.qrEscaneados, estado.totalLugares,
                    estado.vista == VistaDescubrimientos.QR, { onEvent(MisLugaresUiEvent.OnVistaChange(VistaDescubrimientos.QR)) }, Modifier.weight(1f))
                CuadroVista(Icons.Outlined.Videocam, "Cápsulas", estado.capsulasVistas, estado.totalLugares,
                    estado.vista == VistaDescubrimientos.CAPSULAS, { onEvent(MisLugaresUiEvent.OnVistaChange(VistaDescubrimientos.CAPSULAS)) }, Modifier.weight(1f))
                CuadroVista(Icons.Outlined.CameraAlt, "Selfie", estado.selfiesTomadas, estado.totalLugares,
                    estado.vista == VistaDescubrimientos.SELFIES, { onEvent(MisLugaresUiEvent.OnVistaChange(VistaDescubrimientos.SELFIES)) }, Modifier.weight(1f))
            }
        }
        //"Mis lugares" abre P07-A; relleno si es la vista abierta
        item {
            val enLugares = estado.vista == VistaDescubrimientos.LUGARES
            val alPresionar = { onEvent(MisLugaresUiEvent.OnVistaChange(VistaDescubrimientos.LUGARES)) }
            val contenido: @Composable () -> Unit = {
                Icon(Icons.Outlined.Place, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mis lugares")
            }
            if (enLugares) {
                Button(onClick = alPresionar, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { contenido() }
            } else {
                OutlinedButton(
                    onClick = alPresionar,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { contenido() }
            }
        }

        item { MensajeError(mensaje = estado.mensajeError) }

        //la lista y "Por descubrir" cambian con la vista; la transición respeta "Quitar animaciones"
        item {
            AnimatedContent(
                targetState = estado.vista,
                transitionSpec = {
                    if (reducirMovimiento) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        //se desliza hacia el lado de la vista elegida
                        val haciaLaDerecha = targetState.ordinal > initialState.ordinal
                        (fadeIn(tween(250)) + slideInHorizontally(tween(250)) { if (haciaLaDerecha) it / 4 else -it / 4 }) togetherWith
                            (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { if (haciaLaDerecha) -it / 4 else it / 4 })
                    }
                },
                label = "vista"
            ) { vista ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EncabezadoLista(titulo = vista.tituloLista, estado = estado, onEvent = onEvent)
                    when (vista) {
                        VistaDescubrimientos.LUGARES -> ListaLugares(estado = estado, alVerDetalle = alVerDetalle, alIrAlMapa = alIrAlMapa)
                        VistaDescubrimientos.CAPSULAS -> ListaCapsulas(
                            capsulas = estado.capsulas,
                            alVerCapsula = alVerCapsula,
                            textoVacio = textoVacio(estado.filtro, "Escanea un lugar para desbloquear su cápsula.")
                        )
                        VistaDescubrimientos.QR -> ListaQr(
                            qrs = estado.qrs,
                            alVerDetalle = alVerDetalle,
                            textoVacio = textoVacio(estado.filtro, "Todavía no escaneas ningún QR. Búscalos en el mapa.")
                        )
                        VistaDescubrimientos.SELFIES -> ListaSelfies(
                            selfies = estado.selfies,
                            alVerDetalle = alVerDetalle,
                            textoVacio = textoVacio(estado.filtro, "Todavía no tienes selfies. Tómate una en el detalle de un lugar desbloqueado.")
                        )
                    }
                    PorDescubrir(vista = vista, lugares = estado.porDescubrir, alVerEnMapa = { onEvent(MisLugaresUiEvent.OnVerEnMapa(it)) })
                }
            }
        }
    }
}

private fun textoVacio(filtro: FiltroLugares, sinNada: String): String =
    if (filtro == FiltroLugares.TODOS) sinNada else "No hay nada con este filtro."

//título de la lista con los menús de filtrar y ordenar (LUG-03)
@Composable
private fun EncabezadoLista(titulo: String, estado: MisLugaresUiState, onEvent: (MisLugaresUiEvent) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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

//P07-A (LUG-02): sin lugares, invita a ir al mapa
@Composable
private fun ListaLugares(estado: MisLugaresUiState, alVerDetalle: (String) -> Unit, alIrAlMapa: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (estado.desbloqueados.isEmpty() && !estado.cargando) {
            if (estado.filtro == FiltroLugares.TODOS) {
                TextoVacio("Todavía no escaneas ningún lugar. Búscalos en el mapa.")
                TextButton(onClick = alIrAlMapa) { Text("Ir al mapa") }
            } else {
                TextoVacio("No hay lugares con este filtro.")
            }
        }
        estado.desbloqueados.forEach { lugar ->
            TarjetaLugar(lugar = lugar, alPresionar = { alVerDetalle(lugar.puntoId) })
        }
    }
}

//LUG-03: cuántos faltan y una tarjeta por cada uno; tocarla abre el mapa en ese lugar
@Composable
private fun PorDescubrir(vista: VistaDescubrimientos, lugares: List<LugarPorDescubrir>, alVerEnMapa: (String) -> Unit) {
    if (lugares.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = "Por descubrir", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = "${lugares.size} ${if (lugares.size == 1) vista.singular else vista.plural}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        lugares.forEach { lugar ->
            TarjetaPorDescubrir(
                texto = vista.textoPorDescubrir(lugar.nombre),
                pista = vista.pista,
                alPresionar = { alVerEnMapa(lugar.puntoId) }
            )
        }
    }
}

//"Lugares escaneados" y "Mis puntos" (con "Ver ranking ›")
@Composable
private fun TarjetaTotal(
    titulo: String,
    cifra: String,
    detalle: String?,
    fondo: Color,
    texto: Color,
    modifier: Modifier = Modifier,
    alPresionar: (() -> Unit)? = null,
    enlace: String? = null
) {
    val contenido: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = titulo, style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = cifra, style = MaterialTheme.typography.displaySmall)
                if (detalle != null) {
                    Text(text = " $detalle", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            if (enlace != null) Text(text = enlace, style = MaterialTheme.typography.labelLarge)
        }
    }
    if (alPresionar != null) {
        Surface(onClick = alPresionar, color = fondo, contentColor = texto, shape = RoundedCornerShape(16.dp), modifier = modifier) { contenido() }
    } else {
        Surface(color = fondo, contentColor = texto, shape = RoundedCornerShape(16.dp), modifier = modifier) { contenido() }
    }
}

//Cuadro "QR", "Cápsulas" o "Selfie" con su "X de Y". El de la vista abierta se ve relleno
@Composable
private fun CuadroVista(
    icono: ImageVector,
    titulo: String,
    cantidad: Int,
    total: Int,
    elegido: Boolean,
    alPresionar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = alPresionar,
        shape = RoundedCornerShape(14.dp),
        color = if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (elegido) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .heightIn(min = 64.dp)
            //TalkBack dice cuál está elegido
            .semantics {
                role = Role.Tab
                selected = elegido
            }
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(titulo, style = MaterialTheme.typography.labelLarge)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$cantidad", style = MaterialTheme.typography.titleLarge)
                Text(" de $total", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 2.dp))
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = lugar.nombre, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Etiqueta(texto = lugar.tipo)
                    Text(
                        text = lugar.estado.texto,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (completado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            EsferaAgua(puntos = lugar.puntos, maximo = lugar.maximo)
        }
    }
}

@Composable
private fun TarjetaPorDescubrir(texto: String, pista: String, alPresionar: () -> Unit) {
    Surface(
        onClick = alPresionar,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            ) {
                Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(text = texto, style = MaterialTheme.typography.titleMedium)
                Text(text = pista, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = "›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

//etiqueta pequeña con fondo: tipo del lugar, categoría de la cápsula, "Privada"
@Composable
internal fun Etiqueta(
    texto: String,
    fondo: Color = MaterialTheme.colorScheme.surfaceVariant,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .background(fondo, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

//"+10", "+20": los puntos que dio la actividad, en el color de los puntos
@Composable
internal fun EtiquetaPuntos(puntos: Int) {
    Etiqueta(texto = "+$puntos", fondo = MaterialTheme.colorScheme.secondary, color = MaterialTheme.colorScheme.onSecondary)
}

@Composable
internal fun TextoVacio(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private val estadoPreview: MisLugaresUiState
    get() {
        val ahora = System.currentTimeMillis()
        return MisLugaresUiState(
            totalPuntos = 160,
            lugaresEscaneados = 3,
            totalLugares = 14,
            qrEscaneados = 3,
            capsulasVistas = 2,
            selfiesTomadas = 1,
            desbloqueados = listOf(
                LugarDesbloqueado("p1", "La Moneda", "Monumento", 10, 80, EstadoLugar.RECIEN_DESCUBIERTO, ahora),
                LugarDesbloqueado("p2", "Museo de Bellas Artes", "Monumento", 60, 80, EstadoLugar.FALTA_SELFIE, ahora - 1),
                LugarDesbloqueado("p3", "Museo de Arte Precolombino", "Monumento", 80, 80, EstadoLugar.COMPLETADO, ahora - 2)
            ),
            capsulas = listOf(
                CapsulaDescubierta("p1", "c1", "¿Qué es la inteligencia artificial?", "¿Qué es la IA?", "La Moneda", false, ahora),
                CapsulaDescubierta("p3", "c3", "Cómo aprende una máquina", "¿Qué es la IA?", "Museo de Arte Precolombino", true, ahora - 2)
            ),
            qrs = listOf(QrEscaneado("p1", "La Moneda", ahora, 18)),
            porDescubrir = listOf(LugarPorDescubrir("p4", "Plaza de Armas"), LugarPorDescubrir("p5", "Cerro Santa Lucía")),
            cargando = false
        )
    }

@Preview(showBackground = true, heightDp = 1100)
@Composable
fun MisLugaresPreview() {
    TriskeleduTheme {
        Surface {
            MisLugaresContent(estado = estadoPreview, onEvent = {}, alVerDetalle = {}, alAbrirConfiguracion = {}, alVerRanking = {})
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
fun MisCapsulasPreview() {
    TriskeleduTheme {
        Surface {
            MisLugaresContent(
                estado = estadoPreview.copy(vista = VistaDescubrimientos.CAPSULAS),
                onEvent = {},
                alVerDetalle = {},
                alAbrirConfiguracion = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MisLugaresVacioPreview() {
    TriskeleduTheme {
        Surface {
            MisLugaresContent(
                estado = MisLugaresUiState(totalLugares = 14, porDescubrir = listOf(LugarPorDescubrir("p1", "La Moneda")), cargando = false),
                onEvent = {},
                alVerDetalle = {},
                alAbrirConfiguracion = {}
            )
        }
    }
}
