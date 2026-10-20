package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.cookingplastic.triskeledu.ui.event.EscanearUiEvent
import com.cookingplastic.triskeledu.ui.state.EscanearUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

//Guarda el estado de Escanear. Como es un ViewModel, no se pierde al girar el teléfono
class EscanearViewModel : ViewModel() {

    //privado y mutable: solo el ViewModel lo cambia
    private val _estado = MutableStateFlow(EscanearUiState())
    //público y de solo lectura: la pantalla lo observa
    val estado: StateFlow<EscanearUiState> = _estado.asStateFlow()

    fun onEvent(evento: EscanearUiEvent) {
        when (evento) {
            is EscanearUiEvent.OnPermisoCamara -> _estado.update { it.copy(permisoCamara = evento.concedido) }
            is EscanearUiEvent.OnCodigoLeido -> codigoLeido(evento.codigo)
            EscanearUiEvent.OnEscanearOtro -> _estado.update { it.copy(codigoLeido = null) }
        }
    }

    private fun codigoLeido(codigo: String) {
        //la cámara ve el mismo QR muchas veces por segundo: solo se toma el primero
        if (_estado.value.codigoLeido != null) return
        val limpio = codigo.trim()
        if (limpio.isEmpty()) return
        _estado.update { it.copy(codigoLeido = limpio) }
        //siguiente paso, cuando Simon tenga PuntoInteresRepository y UbicacionRepository:
        //buscar el punto por su código, medir la distancia y aplicar ReglaEscaneo (60 m)
    }
}