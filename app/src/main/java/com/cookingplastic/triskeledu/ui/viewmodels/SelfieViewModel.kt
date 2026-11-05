package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.model.PUNTOS_SELFIE
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.ui.event.SelfieUiEvent
import com.cookingplastic.triskeledu.ui.state.SelfieUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//P13: no tiene la cámara (esa vive en la pantalla). Revisa que el punto esté desbloqueado y, con la foto
//ya guardada, la registra y suma 20 una sola vez (ACT-03). Si cruza un nivel aparece P25 (ProgresoConNivel)
class SelfieViewModel(
    private val puntoId: String,
    private val puntoInteresRepository: PuntoInteresRepository,
    private val usuarioRepository: UsuarioRepository,
    private val progresoRepository: ProgresoRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(SelfieUiState())
    val estado: StateFlow<SelfieUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val punto = puntoInteresRepository.obtenerPorId(puntoId).first()
            val usuario = usuarioRepository.usuarioActual().first()
            val fila = usuario?.let { progresoRepository.progresoDePunto(it.id, puntoId).first() }
            _estado.update {
                it.copy(
                    nombrePunto = punto?.nombre.orEmpty(),
                    desbloqueado = fila != null,
                    yaTomada = fila?.selfieEn != null,
                    cargando = false
                )
            }
        }
    }

    fun onEvent(evento: SelfieUiEvent) {
        when (evento) {
            is SelfieUiEvent.OnPermisoCamara -> _estado.update { it.copy(permisoCamara = evento.concedido, permisoBloqueado = evento.bloqueado) }
            SelfieUiEvent.OnCambiarCamara -> _estado.update { it.copy(camaraFrontal = !it.camaraFrontal) }
            SelfieUiEvent.OnTomandoFoto -> _estado.update { it.copy(guardando = true, mensajeError = null) }
            is SelfieUiEvent.OnFotoGuardada -> registrar(evento.uri)
            SelfieUiEvent.OnErrorCamara -> _estado.update { it.copy(guardando = false, mensajeError = "No se pudo tomar la foto. Intenta de nuevo") }
        }
    }

    private fun registrar(uri: String) {
        viewModelScope.launch {
            val usuario = usuarioRepository.usuarioActual().first()
            if (usuario == null) {
                _estado.update { it.copy(guardando = false, mensajeError = "Inicia sesión para guardar tu selfie") }
                return@launch
            }
            try {
                val sumo = progresoRepository.registrarSelfie(usuario.id, puntoId, uri)
                _estado.update {
                    if (sumo) it.copy(guardando = false, yaTomada = true, puntosGanados = PUNTOS_SELFIE)
                    //ya tenía selfie o el punto no está escaneado: no se suma de nuevo
                    else it.copy(guardando = false, mensajeError = if (it.desbloqueado) "Ya subiste tu selfie en este lugar" else MENSAJE_BLOQUEADO)
                }
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, mensajeError = "No se pudo guardar la selfie") }
            }
        }
    }

    companion object {
        const val MENSAJE_BLOQUEADO = "Escanea el QR del lugar para desbloquear esta actividad"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                SelfieViewModel(
                    puntoId = createSavedStateHandle().get<String>(Rutas.ARG_PUNTO_ID).orEmpty(),
                    puntoInteresRepository = app.container.puntoInteresRepository,
                    usuarioRepository = app.container.usuarioRepository,
                    progresoRepository = RepositoriosProgreso.progreso(app)
                )
            }
        }
    }
}
