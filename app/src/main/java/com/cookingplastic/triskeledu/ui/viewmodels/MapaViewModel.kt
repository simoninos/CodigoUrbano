package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosCompartidos
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.location.UbicacionRepository
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.model.puntosTotales
import com.cookingplastic.triskeledu.data.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.event.MapaUiEvent
import com.cookingplastic.triskeledu.ui.state.MapaUiState
import com.cookingplastic.triskeledu.ui.state.PuntoMapa
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel de la pantalla 3 (Mapa).
// Junta cuatro fuentes en un solo estado: puntos activos, ubicación, usuario conectado con su
// progreso, y preferencias.
@OptIn(ExperimentalCoroutinesApi::class)
class MapaViewModel(
    private val puntoInteresRepository: PuntoInteresRepository,
    private val ubicacionRepository: UbicacionRepository,
    private val usuarioRepository: UsuarioRepository,
    private val preferenciasRepository: PreferenciasRepository,
    private val progresoRepository: ProgresoRepository
) : ViewModel() {

    // Privado y mutable: solo el ViewModel escribe. Público y de solo lectura: la pantalla observa.
    private val _estado = MutableStateFlow(MapaUiState())
    val estado: StateFlow<MapaUiState> = _estado.asStateFlow()

    // U-04: la ubicación se escucha solo con permiso. Cuando el permiso cambia,
    // flatMapLatest deja el Flow anterior y empieza uno nuevo.
    private val permisoConcedido = MutableStateFlow(false)

    private val ubicacion: Flow<Ubicacion?> = permisoConcedido.flatMapLatest { concedido ->
        if (!concedido) {
            flowOf(null)
        } else {
            ubicacionRepository.ubicaciones()
                .map<Ubicacion, Ubicacion?> { it }
                .onStart { emit(ubicacionRepository.ultimaUbicacion()) }
                .catch {
                    // U-06 y M-18: sin ubicación, el mapa sigue funcionando y avisa con un mensaje
                    _estado.update { it.copy(mensajeError = "No se pudo obtener tu ubicación. Revisa que el GPS esté activo") }
                    emit(null)
                }
        }
    }

    // El progreso depende del usuario conectado: al cambiar de usuario, flatMapLatest deja de
    // escuchar el progreso anterior y empieza con el del nuevo. Sin sesión no hay progreso.
    private val usuarioConProgreso: Flow<Pair<Usuario?, List<ProgresoPunto>>> =
        usuarioRepository.usuarioActual().flatMapLatest { usuario ->
            if (usuario == null) {
                flowOf(null to emptyList())
            } else {
                progresoRepository.progresoDe(usuario.id).map { filas -> usuario to filas }
            }
        }

    // Últimas preferencias recibidas; el botón de seguimiento guarda una copia con el valor cambiado
    private var preferenciasActuales: Preferencias? = null

    init {
        viewModelScope.launch {
            // combine emite un estado nuevo cada vez que cualquiera de las cuatro fuentes cambia
            combine(
                puntoInteresRepository.obtenerActivos(),
                ubicacion,
                usuarioConProgreso,
                preferenciasRepository.preferencias()
            ) { puntos, ubicacionActual, (usuario, progreso), preferencias ->
                preferenciasActuales = preferencias
                // Para buscar rápido la fila de cada punto: puntoId -> fila
                val progresoPorPunto = progreso.associateBy { it.puntoId }
                val puntosMapa = puntos.map { punto ->
                    PuntoMapa(
                        punto = punto,
                        // M-04: el estado sale del progreso del usuario en ese punto
                        estado = EstadoPunto.desde(progresoPorPunto[punto.id]),
                        distanciaMetros = ubicacionActual?.let {
                            ubicacionRepository.distanciaMetros(it, punto.ubicacion)
                        }
                    )
                }
                _estado.update {
                    it.copy(
                        puntos = puntosMapa,
                        ubicacionUsuario = ubicacionActual ?: it.ubicacionUsuario,
                        nombreUsuario = usuario?.username ?: "",
                        // M-13: suma todo lo ganado, igual que Mis lugares (también en puntos ya desactivados, RN-15)
                        totalPuntos = progreso.sumOf { it.puntosTotales() },
                        seguirUbicacion = preferencias.seguirUbicacion,
                        mostrarBloqueados = preferencias.mostrarBloqueados,
                        cargando = false
                    )
                }
            }
                .catch {
                    _estado.update { it.copy(cargando = false, mensajeError = "No se pudieron cargar los puntos") }
                }
                .collect { }
        }
    }

    fun onEvent(evento: MapaUiEvent) {
        when (evento) {
            is MapaUiEvent.OnClickPunto -> _estado.update { it.copy(seleccionadoId = evento.puntoId) }
            MapaUiEvent.OnCerrarTarjeta -> _estado.update { it.copy(seleccionadoId = null) }
            MapaUiEvent.OnCentrar -> {
                if (_estado.value.ubicacionUsuario == null) {
                    _estado.update { it.copy(mensajeError = "Todavía no tenemos tu ubicación") }
                } else {
                    _estado.update { it.copy(solicitudCentrar = it.solicitudCentrar + 1) }
                }
            }
            MapaUiEvent.OnAlternarSeguimiento -> alternarSeguimiento()
            is MapaUiEvent.OnCamaraGuardada -> _estado.update { it.copy(camara = evento.camara) }
            MapaUiEvent.OnErrorMostrado -> _estado.update { it.copy(mensajeError = null) }
            is MapaUiEvent.OnPermisoUbicacion -> {
                permisoConcedido.value = evento.concedido
                if (!evento.concedido) {
                    // U-05
                    _estado.update {
                        it.copy(mensajeError = "Sin permiso de ubicación no podemos mostrar dónde estás ni la distancia a los lugares")
                    }
                }
            }
        }
    }

    // M-20 y M-21: cambia la preferencia seguirUbicacion, la misma del interruptor de Configuración
    private fun alternarSeguimiento() {
        val actuales = preferenciasActuales ?: return
        val seguir = !actuales.seguirUbicacion
        viewModelScope.launch {
            try {
                // El Flow de preferencias trae el cambio de vuelta y actualiza el estado
                preferenciasRepository.guardar(actuales.copy(seguirUbicacion = seguir))
                _estado.update {
                    it.copy(
                        // M-22: al activarlo, el mapa vuelve al usuario de inmediato
                        solicitudCentrar = if (seguir && it.ubicacionUsuario != null) it.solicitudCentrar + 1 else it.solicitudCentrar,
                        mensajeError = if (seguir) "Seguimiento activado" else "Seguimiento desactivado"
                    )
                }
            } catch (e: Exception) {
                _estado.update { it.copy(mensajeError = "No se pudo cambiar el seguimiento") }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                MapaViewModel(
                    puntoInteresRepository = app.container.puntoInteresRepository,
                    ubicacionRepository = RepositoriosCompartidos.ubicacion(app),
                    usuarioRepository = app.container.usuarioRepository,
                    preferenciasRepository = app.container.preferenciasRepository,
                    progresoRepository = RepositoriosProgreso.progreso(app)
                )
            }
        }
    }
}
