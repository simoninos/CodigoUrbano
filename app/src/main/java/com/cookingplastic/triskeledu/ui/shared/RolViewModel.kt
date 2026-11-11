package com.cookingplastic.triskeledu.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.repository.NotificacionRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

//Vive en AppScaffold: le dice a la barra inferior y a las pestañas qué rol tiene el usuario conectado.
//MAP-01: el administrador no tiene Escanear y su pestaña Mis lugares es P07-Admin
class RolViewModel(usuarioRepository: UsuarioRepository, notificacionRepository: NotificacionRepository) : ViewModel() {

    //null sin sesión
    val rol: StateFlow<RolUsuario?> = usuarioRepository.usuarioActual()
        .map { it?.rol }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    //ADM-10: el contador de la campana del administrador en P07-Admin
    @OptIn(ExperimentalCoroutinesApi::class)
    val solicitudesPendientes: StateFlow<Int> = usuarioRepository.usuarioActual()
        .flatMapLatest { usuario ->
            if (usuario?.rol == RolUsuario.ADMINISTRADOR) usuarioRepository.solicitudesPendientes().map { it.size } else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    //PER-03: el contador de la campana de Mis lugares (el Mapa lo calcula en su propio estado)
    @OptIn(ExperimentalCoroutinesApi::class)
    val notificacionesSinLeer: StateFlow<Int> = usuarioRepository.usuarioActual()
        .flatMapLatest { usuario -> if (usuario == null) flowOf(0) else notificacionRepository.sinLeer(usuario.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                RolViewModel(app.container.usuarioRepository, app.container.notificacionRepository)
            }
        }
    }
}
