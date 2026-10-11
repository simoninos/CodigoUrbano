package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookingplastic.triskeledu.data.model.MonumentoUiState
import com.cookingplastic.triskeledu.data.repository.MonumentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MonumentoViewModel(private val repository: MonumentoRepository) : ViewModel() {
    private val _estado = MutableStateFlow(MonumentoUiState())
    val estado: StateFlow<MonumentoUiState> = _estado.asStateFlow()

    init {
        cargarDatosMonumentos()
    }

    private fun cargarDatosMonumentos(){
        viewModelScope.launch {
            try {
                _estado.update { it.copy(cargando = true, mensajeError = null) }
                val monumentos = repository.getMonumentos()
                _estado.update { it.copy(cargando = false, mensajeError = null,
                    monumentos = monumentos) }
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, mensajeError = "Ocurrio un error al cargar los monumentos") }
            }
        }
    }
}