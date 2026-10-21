package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosCompartidos
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.model.PUNTOS_MAXIMOS_LUGAR
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.estaCompletado
import com.cookingplastic.triskeledu.data.model.puntosTotales
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.event.MisLugaresUiEvent
import com.cookingplastic.triskeledu.ui.state.EstadoLugar
import com.cookingplastic.triskeledu.ui.state.FiltroLugares
import com.cookingplastic.triskeledu.ui.state.LugarDesbloqueado
import com.cookingplastic.triskeledu.ui.state.MisLugaresUiState
import com.cookingplastic.triskeledu.ui.state.OrdenLugares
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//Un lugar escaneado hace menos de un día se muestra como "Recién descubierto"
private const val UN_DIA_MS = 24 * 60 * 60 * 1000L

//Recibe tres fuentes de datos: el progreso (Catalina), el usuario conectado (Gabriel)
//y la lista de lugares activos (obtenerActivos() de PuntoInteresRepository, de Simon)
class MisLugaresViewModel(
    private val progresoRepository: ProgresoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val puntosActivos: Flow<List<PuntoInteres>>
) : ViewModel() {

    private val _estado = MutableStateFlow(MisLugaresUiState())
    val estado: StateFlow<MisLugaresUiState> = _estado.asStateFlow()

    //lo que eligió el usuario en los menús de filtrar y ordenar
    private val filtro = MutableStateFlow(FiltroLugares.TODOS)
    private val orden = MutableStateFlow(OrdenLugares.RECIENTES)

    //cada vez que cambia el usuario conectado, se pasa a escuchar el progreso de ese usuario
    @OptIn(ExperimentalCoroutinesApi::class)
    private val progreso: Flow<List<ProgresoPunto>> =
        usuarioRepository.usuarioActual().flatMapLatest { usuario ->
            if (usuario == null) flowOf(emptyList()) else progresoRepository.progresoDe(usuario.id)
        }

    init {
        viewModelScope.launch {
            //combine junta las 4 fuentes: si cualquiera cambia, se vuelve a armar la pantalla
            combine(puntosActivos, progreso, filtro, orden) { puntos, filas, filtroActual, ordenActual ->
                armarMisLugares(puntos, filas, filtroActual, ordenActual, System.currentTimeMillis())
            }
                .catch { error ->
                    _estado.update { it.copy(cargando = false, mensajeError = "No se pudieron cargar tus lugares") }
                }
                .collect { nuevo -> _estado.value = nuevo }
        }
    }

    fun onEvent(evento: MisLugaresUiEvent) {
        when (evento) {
            is MisLugaresUiEvent.OnFiltroChange -> filtro.value = evento.filtro
            is MisLugaresUiEvent.OnOrdenChange -> orden.value = evento.orden
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                MisLugaresViewModel(
                    progresoRepository = RepositoriosProgreso.progreso(app),
                    usuarioRepository = app.container.usuarioRepository,
                    puntosActivos = RepositoriosCompartidos.puntoInteres.obtenerActivos()
                )
            }
        }
    }
}

//Arma todo lo que muestra Mis lugares a partir de los datos.
//No usa Android ni corrutinas: recibe listas y devuelve el estado, así se prueba fácil
fun armarMisLugares(
    puntos: List<PuntoInteres>,
    progreso: List<ProgresoPunto>,
    filtro: FiltroLugares,
    orden: OrdenLugares,
    ahora: Long
): MisLugaresUiState {
    //para buscar rápido la fila de progreso de cada lugar: puntoId -> fila
    val progresoPorPunto = progreso.associateBy { it.puntoId }

    //solo los lugares que tienen fila de progreso están desbloqueados
    val desbloqueados = puntos.mapNotNull { punto ->
        val fila = progresoPorPunto[punto.id]
        if (fila == null) {
            null
        } else {
            LugarDesbloqueado(
                puntoId = punto.id,
                nombre = punto.nombre,
                tipo = punto.tipo.descripcion,
                puntos = fila.puntosTotales(),
                maximo = PUNTOS_MAXIMOS_LUGAR,
                estado = when {
                    fila.estaCompletado() -> EstadoLugar.COMPLETADO
                    ahora - fila.escaneadoEn < UN_DIA_MS -> EstadoLugar.RECIEN_DESCUBIERTO
                    else -> EstadoLugar.FALTA_VIDEO
                },
                escaneadoEn = fila.escaneadoEn
            )
        }
    }

    val filtrados = when (filtro) {
        FiltroLugares.TODOS -> desbloqueados
        FiltroLugares.EN_CURSO -> desbloqueados.filter { it.estado != EstadoLugar.COMPLETADO }
        FiltroLugares.COMPLETADOS -> desbloqueados.filter { it.estado == EstadoLugar.COMPLETADO }
    }

    val ordenados = when (orden) {
        OrdenLugares.RECIENTES -> filtrados.sortedByDescending { it.escaneadoEn }
        OrdenLugares.NOMBRE -> filtrados.sortedBy { it.nombre }
        OrdenLugares.PUNTOS -> filtrados.sortedByDescending { it.puntos }
    }

    return MisLugaresUiState(
        //los totales cuentan todo lo ganado, sin importar el filtro elegido
        totalPuntos = progreso.sumOf { it.puntosTotales() },
        lugaresEscaneados = desbloqueados.size,
        totalLugares = puntos.size,
        desbloqueados = ordenados,
        bloqueados = puntos.size - desbloqueados.size,
        filtro = filtro,
        orden = orden,
        cargando = false
    )
}