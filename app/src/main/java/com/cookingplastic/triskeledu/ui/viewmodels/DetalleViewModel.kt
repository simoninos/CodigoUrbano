package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.data.RepositoriosCompartidos
import com.cookingplastic.triskeledu.data.location.UbicacionRepository
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.red.AndroidRedRepository
import com.cookingplastic.triskeledu.data.red.RedRepository
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.ui.event.DetalleUiEvent
import com.cookingplastic.triskeledu.ui.state.DetalleUiState
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

// ViewModel de la pantalla 4 (Detalle del punto).
// El id del punto llega en la ruta "detalle/{puntoId}" y se lee desde SavedStateHandle.
class DetalleViewModel(
    savedStateHandle: SavedStateHandle,
    private val puntoInteresRepository: PuntoInteresRepository,
    private val ubicacionRepository: UbicacionRepository,
    private val redRepository: RedRepository
) : ViewModel() {

    private val puntoId: String = savedStateHandle.get<String>(Rutas.ARG_PUNTO_ID).orEmpty()

    private val _estado = MutableStateFlow(DetalleUiState())
    val estado: StateFlow<DetalleUiState> = _estado.asStateFlow()

    // Distancia en vivo; sin permiso o sin GPS se sigue sin distancia (RNF-11)
    private val ubicacion: Flow<Ubicacion?> = ubicacionRepository.ubicaciones()
        .map<Ubicacion, Ubicacion?> { it }
        .onStart { emit(ubicacionRepository.ultimaUbicacion()) }
        .catch { emit(null) }

    init {
        viewModelScope.launch {
            combine(puntoInteresRepository.obtenerPorId(puntoId), ubicacion) { punto, ubicacionActual ->
                _estado.update {
                    it.copy(
                        punto = punto,
                        // Sin ProgresoRepository todos los puntos están bloqueados
                        estadoPunto = EstadoPunto.BLOQUEADO,
                        distanciaMetros = if (punto != null && ubicacionActual != null) {
                            ubicacionRepository.distanciaMetros(ubicacionActual, punto.ubicacion)
                        } else {
                            it.distanciaMetros
                        },
                        cargando = false,
                        // D-01
                        noEncontrado = punto == null
                    )
                }
            }
                .catch { _estado.update { it.copy(cargando = false, mensaje = "No se pudo cargar el lugar") } }
                .collect { }
        }
    }

    fun onEvent(evento: DetalleUiEvent) {
        when (evento) {
            DetalleUiEvent.OnVerVideo -> verVideo()
            DetalleUiEvent.OnTomarSelfie -> actividadDesbloqueable()
            DetalleUiEvent.OnComentar -> actividadDesbloqueable()
            DetalleUiEvent.OnCerrarSinConexion -> _estado.update { it.copy(mostrarSinConexion = false) }
            DetalleUiEvent.OnMensajeMostrado -> _estado.update { it.copy(mensaje = null) }
            DetalleUiEvent.OnEnlaceNoDisponible -> _estado.update { it.copy(mensaje = "No se pudo abrir el enlace") }
        }
    }

    private fun verVideo() {
        // D-10: el video es streaming; sin internet se muestra el popup, sea cual sea el estado del punto
        if (!redRepository.hayConexion()) {
            _estado.update { it.copy(mostrarSinConexion = true) }
            return
        }
        actividadDesbloqueable()
    }

    // D-11: video, selfie y comentario se desbloquean al escanear el QR del punto (RN-07)
    private fun actividadDesbloqueable() {
        val mensaje = if (_estado.value.desbloqueado) {
            "Esta actividad estará disponible en la próxima versión"
        } else {
            "Escanea el QR del lugar para desbloquear esta actividad"
        }
        _estado.update { it.copy(mensaje = mensaje) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                DetalleViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    puntoInteresRepository = RepositoriosCompartidos.puntoInteres,
                    ubicacionRepository = RepositoriosCompartidos.ubicacion(app),
                    redRepository = AndroidRedRepository(app)
                )
            }
        }
    }
}
