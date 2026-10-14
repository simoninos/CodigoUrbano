package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.state.MapaUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MapaViewModel(private val repository: UsuarioRepository): ViewModel() {
    private val _estado = MutableStateFlow(MapaViewModel(
        repository = TODO()
    ))
    val estado: StateFlow<MapaViewModel> = _estado.asStateFlow()



}