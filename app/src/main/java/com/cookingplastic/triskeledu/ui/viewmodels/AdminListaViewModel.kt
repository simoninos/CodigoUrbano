package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.data.RepositoriosCompartidos
import com.cookingplastic.triskeledu.data.model.datosFaltantes
import com.cookingplastic.triskeledu.data.model.puedeActivarse
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.ui.event.AdminListaUiEvent
import com.cookingplastic.triskeledu.ui.state.AdminListaUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel de la pantalla 9 (Admin: puntos de interés): listar, buscar, activar o desactivar y eliminar
class AdminListaViewModel(
    private val puntoInteresRepository: PuntoInteresRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(AdminListaUiState())
    val estado: StateFlow<AdminListaUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            // A-02: todos los puntos, activos e inactivos, ordenados por nombre
            puntoInteresRepository.obtenerTodos()
                .catch { _estado.update { it.copy(cargando = false, mensaje = "No se pudieron cargar los puntos") } }
                .collect { puntos ->
                    _estado.update { it.copy(puntos = puntos.sortedBy { p -> p.nombre }, cargando = false) }
                }
        }
    }

    fun onEvent(evento: AdminListaUiEvent) {
        when (evento) {
            is AdminListaUiEvent.OnBusquedaChange -> _estado.update { it.copy(busqueda = evento.valor) }
            is AdminListaUiEvent.OnCambiarActivo -> cambiarActivo(evento.puntoId, evento.activo)
            is AdminListaUiEvent.OnPedirEliminar -> _estado.update { it.copy(puntoAEliminar = evento.punto) }
            AdminListaUiEvent.OnCancelarEliminar -> _estado.update { it.copy(puntoAEliminar = null) }
            AdminListaUiEvent.OnConfirmarEliminar -> eliminar()
            AdminListaUiEvent.OnMensajeMostrado -> _estado.update { it.copy(mensaje = null) }
        }
    }

    private fun cambiarActivo(puntoId: String, activo: Boolean) {
        val punto = _estado.value.puntos.find { it.id == puntoId } ?: return
        // A-07: un punto incompleto no se activa y se dice qué le falta
        if (activo && !punto.puedeActivarse()) {
            _estado.update { it.copy(mensaje = "No se puede activar. Falta: ${unirConY(punto.datosFaltantes())}") }
            return
        }
        viewModelScope.launch {
            try {
                // Si funciona, el Flow de obtenerTodos trae el cambio; no hay que tocar el estado aquí
                if (!puntoInteresRepository.cambiarActivo(puntoId, activo)) {
                    _estado.update { it.copy(mensaje = "No se pudo cambiar el estado del punto") }
                }
            } catch (e: Exception) {
                _estado.update { it.copy(mensaje = "No se pudo cambiar el estado del punto") }
            }
        }
    }

    private fun eliminar() {
        val punto = _estado.value.puntoAEliminar ?: return
        _estado.update { it.copy(puntoAEliminar = null) }
        viewModelScope.launch {
            try {
                puntoInteresRepository.eliminar(punto.id)
                _estado.update { it.copy(mensaje = "Se eliminó «${punto.nombre}»") }
            } catch (e: Exception) {
                _estado.update { it.copy(mensaje = "No se pudo eliminar el punto") }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AdminListaViewModel(RepositoriosCompartidos.puntoInteres)
            }
        }
    }
}

// ["imagen", "video", "QR"] -> "imagen, video y QR"
fun unirConY(partes: List<String>): String = when (partes.size) {
    0 -> ""
    1 -> partes[0]
    else -> partes.dropLast(1).joinToString(", ") + " y " + partes.last()
}
