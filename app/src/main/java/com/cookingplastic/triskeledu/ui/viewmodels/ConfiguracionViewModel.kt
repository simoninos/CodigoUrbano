package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.event.ConfiguracionUiEvent
import com.cookingplastic.triskeledu.ui.state.ConfiguracionUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//Recibe dos repositorios: uno para las preferencias y otro para saber quién está conectado
class ConfiguracionViewModel(
    private val preferenciasRepository: PreferenciasRepository,
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(ConfiguracionUiState())
    val estado: StateFlow<ConfiguracionUiState> = _estado.asStateFlow()

    //dos tareas en segundo plano, una por cada Flow que hay que escuchar
    init {
        viewModelScope.launch {
            preferenciasRepository.preferencias().collect { preferencias ->
                _estado.update { it.copy(preferencias = preferencias, cargando = false) }
            }
        }
        viewModelScope.launch {
            usuarioRepository.usuarioActual().collect { usuario ->
                _estado.update {
                    it.copy(
                        nombreUsuario = usuario?.username ?: "",
                        esAdministrador = usuario?.rol == RolUsuario.ADMINISTRADOR,
                        //si no hay nadie conectado (por ejemplo, después de cerrar sesión), se vuelve al login
                        sesionCerrada = usuario == null
                    )
                }
            }
        }
    }

    fun onEvent(evento: ConfiguracionUiEvent) {
        //copy cambia solo el interruptor que se tocó; los otros 4 quedan iguales
        val actuales = _estado.value.preferencias
        when (evento) {
            is ConfiguracionUiEvent.OnTemaNocheChange -> guardar(actuales.copy(temaNoche = evento.valor))
            is ConfiguracionUiEvent.OnNotifLugaresCercanosChange -> guardar(actuales.copy(notifLugaresCercanos = evento.valor))
            is ConfiguracionUiEvent.OnNotifNuevosVideosChange -> guardar(actuales.copy(notifNuevosVideos = evento.valor))
            is ConfiguracionUiEvent.OnSeguirUbicacionChange -> guardar(actuales.copy(seguirUbicacion = evento.valor))
            is ConfiguracionUiEvent.OnMostrarBloqueadosChange -> guardar(actuales.copy(mostrarBloqueados = evento.valor))
            ConfiguracionUiEvent.OnCerrarSesion -> cerrarSesion()
        }
    }

    private fun guardar(nuevas: Preferencias) {
        //mientras carga no se guarda: todavía no se sabe de quién son las preferencias
        if (_estado.value.cargando) return

        viewModelScope.launch {
            _estado.update { it.copy(mensajeError = null) }
            try {
                //el estado no se toca aquí: el Flow del init trae lo guardado y actualiza la pantalla
                preferenciasRepository.guardar(nuevas)
            } catch (e: Exception) {
                _estado.update { it.copy(mensajeError = "No se pudo guardar el cambio") }
            }
        }
    }

    private fun cerrarSesion() {
        viewModelScope.launch {
            //usuarioActual avisa null y el init pone sesionCerrada = true
            usuarioRepository.cerrarSesion()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                ConfiguracionViewModel(app.container.preferenciasRepository, app.container.usuarioRepository)
            }
        }
    }
}
