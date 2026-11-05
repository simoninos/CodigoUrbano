package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.data.simulacion.ResultadoSolicitud
import com.cookingplastic.triskeledu.data.simulacion.SolicitudesRepository
import com.cookingplastic.triskeledu.ui.event.SolicitudesUiEvent
import com.cookingplastic.triskeledu.ui.state.DecisionSolicitud
import com.cookingplastic.triskeledu.ui.state.SolicitudesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//P22: el administrador aprueba o rechaza las solicitudes de creador (ADM-10, SIM-02, RN-29)
class SolicitudesViewModel(
    private val solicitudesRepository: SolicitudesRepository,
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(SolicitudesUiState())
    val estado: StateFlow<SolicitudesUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            solicitudesRepository.pendientes().collect { lista -> _estado.update { it.copy(pendientes = lista, cargando = false) } }
        }
    }

    fun onEvent(evento: SolicitudesUiEvent) {
        when (evento) {
            is SolicitudesUiEvent.OnAprobar -> _estado.update { it.copy(porConfirmar = DecisionSolicitud(evento.usuario, aprobar = true)) }
            is SolicitudesUiEvent.OnRechazar -> _estado.update { it.copy(porConfirmar = DecisionSolicitud(evento.usuario, aprobar = false)) }
            SolicitudesUiEvent.OnCancelar -> _estado.update { it.copy(porConfirmar = null) }
            SolicitudesUiEvent.OnConfirmar -> responder()
            SolicitudesUiEvent.OnMensajeVisto -> _estado.update { it.copy(mensaje = null) }
        }
    }

    private fun responder() {
        val decision = _estado.value.porConfirmar ?: return
        if (_estado.value.procesando) return
        _estado.update { it.copy(porConfirmar = null, procesando = true) }
        viewModelScope.launch {
            val admin = usuarioRepository.usuarioActual().first()
            if (admin == null) {
                _estado.update { it.copy(procesando = false, mensaje = "Inicia sesión como administrador para responder") }
                return@launch
            }
            val usuario = decision.usuario
            val resultado = if (decision.aprobar) solicitudesRepository.aprobar(usuario.id, admin.id)
            else solicitudesRepository.rechazar(usuario.id, admin.id)
            val mensaje = when {
                resultado == ResultadoSolicitud.YA_RESPONDIDA -> YA_RESPONDIDA
                decision.aprobar -> "${usuario.username} ya es creador de contenido"
                else -> "Rechazaste la solicitud de ${usuario.username}"
            }
            _estado.update { it.copy(procesando = false, mensaje = mensaje) }
        }
    }

    companion object {
        const val YA_RESPONDIDA = "Esta solicitud ya fue respondida"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                SolicitudesViewModel(app.container.solicitudesRepository, app.container.usuarioRepository)
            }
        }
    }
}
