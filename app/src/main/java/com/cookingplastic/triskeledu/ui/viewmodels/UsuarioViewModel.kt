package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookingplastic.triskeledu.ui.state.UsuarioUiState
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.event.UsuarioUiEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UsuarioViewModel(
    private val repository: UsuarioRepository = UsuarioRepository()
) : ViewModel(){
    private val _estado = MutableStateFlow(UsuarioUiState())
    val estado: StateFlow<UsuarioUiState> = _estado.asStateFlow()

    init {
        cargarDatosUsuario()
    }

    fun onEvent(event: UsuarioUiEvent) {
        when (event) {
            is UsuarioUiEvent.OnPuntosChange -> {
                _estado.update {
                    it.copy(
                        usuario = it.usuario.copy(puntos = event.valor)
                    )
                }
            }

            is UsuarioUiEvent.OnMonumentosEscaneadosChange -> {
                _estado.update {
                    it.copy(
                        usuario = it.usuario.copy(monumentosEscaneados = event.valor)
                    )
                }
            }

            UsuarioUiEvent.OnCargarUsuario -> cargarDatosUsuario()
            UsuarioUiEvent.OnGuardarUsuario -> guardarUsuario()
        }
    }

    private fun cargarDatosUsuario(){
        viewModelScope.launch {
            try {
                _estado.update { it.copy(cargando = true, mensajeError = null)}
                val usuario = repository.getUsuario()
                _estado.update {
                    it.copy(
                        usuario = usuario,
                        cargando = false
                    )
                }

            } catch (error : Exception) {
                _estado.update { it.copy(cargando = false, mensajeError = "No se pudieron cargar los datos")}
            }
        }
    }

    private fun guardarUsuario(){
        viewModelScope.launch {
            try {
                _estado.update { it.copy(cargando = true)}
                repository.guardarUsuario(usuarioActualizado = _estado.value.usuario)
                _estado.update { it.copy(cargando = false)}

            } catch (error : Exception) {
                _estado.update { it.copy (cargando =false, mensajeError = "No se pudieron guardar los datos") }
            }
        }

    }
}