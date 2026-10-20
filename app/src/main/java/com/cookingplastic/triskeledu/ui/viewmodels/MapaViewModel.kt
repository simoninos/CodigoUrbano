package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.location.FakeUbicacionRepository
import com.cookingplastic.triskeledu.data.location.UbicacionRepository
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.event.MapaUiEvent
import com.cookingplastic.triskeledu.ui.state.MapaUiState
import com.cookingplastic.triskeledu.ui.state.PuntoMapa
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel de la pantalla 3 (Mapa).
// Junta cuatro fuentes en un solo estado: puntos activos, ubicación, usuario conectado y preferencias.
class MapaViewModel(
    private val puntoInteresRepository: PuntoInteresRepository,
    private val ubicacionRepository: UbicacionRepository,
    private val usuarioRepository: UsuarioRepository,
    private val preferenciasRepository: PreferenciasRepository
) : ViewModel() {

    // Privado y mutable: solo el ViewModel escribe. Público y de solo lectura: la pantalla observa.
    private val _estado = MutableStateFlow(MapaUiState())
    val estado: StateFlow<MapaUiState> = _estado.asStateFlow()

    // Ubicación del usuario. Si falla, se sigue sin ubicación y se avisa con un mensaje (M-18)
    private val ubicacion: Flow<Ubicacion?> = ubicacionRepository.ubicaciones()
        .map<Ubicacion, Ubicacion?> { it }
        .onStart { emit(ubicacionRepository.ultimaUbicacion()) }
        .catch {
            _estado.update { it.copy(mensajeError = "No se pudo obtener tu ubicación") }
            emit(null)
        }

    init {
        viewModelScope.launch {
            // combine emite un estado nuevo cada vez que cualquiera de las cuatro fuentes cambia
            combine(
                puntoInteresRepository.obtenerActivos(),
                ubicacion,
                usuarioRepository.usuarioActual(),
                preferenciasRepository.preferencias()
            ) { puntos, ubicacionActual, usuario, preferencias ->
                val puntosMapa = puntos.map { punto ->
                    PuntoMapa(
                        punto = punto,
                        // M-04: sin ProgresoRepository todos los puntos están bloqueados
                        estado = EstadoPunto.BLOQUEADO,
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
            MapaUiEvent.OnErrorMostrado -> _estado.update { it.copy(mensajeError = null) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                MapaViewModel(
                    // AppContainer todavía no registra estos dos repositorios: se usan sus versiones Fake
                    puntoInteresRepository = FakePuntoInteresRepository(),
                    ubicacionRepository = FakeUbicacionRepository(),
                    usuarioRepository = app.container.usuarioRepository,
                    preferenciasRepository = app.container.preferenciasRepository
                )
            }
        }
    }
}
