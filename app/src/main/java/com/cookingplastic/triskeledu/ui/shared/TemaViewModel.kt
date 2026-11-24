package com.cookingplastic.triskeledu.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.domain.repository.PreferenciasRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

//Le dice a toda la app qué tema usar y si va de día o de noche, según las preferencias del usuario conectado
class TemaViewModel(preferenciasRepository: PreferenciasRepository) : ViewModel() {

    //stateIn convierte el Flow en StateFlow, que siempre tiene un valor: parte en false (día) mientras llega el guardado.
    //WhileSubscribed(5_000) sigue escuchando 5 segundos sin pantalla, así al girar el teléfono no se corta
    val temaNoche: StateFlow<Boolean> = preferenciasRepository.preferencias()
        .map { it.temaNoche }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    //LVL-03: el tema elegido en Apariencia; sin sesión es el Clásico
    val tema: StateFlow<TemaApp> = preferenciasRepository.preferencias()
        .map { it.tema }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemaApp.CLASICO)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                TemaViewModel(app.container.preferenciasRepository)
            }
        }
    }
}
