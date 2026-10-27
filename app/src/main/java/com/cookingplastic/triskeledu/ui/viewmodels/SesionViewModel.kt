package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.event.SesionUiEvent
import com.cookingplastic.triskeledu.ui.state.SesionUiState
import com.cookingplastic.triskeledu.ui.validation.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//aquí empezamos a ver
//Recibe a usuarioRepository que esté en el momento (Hoy Fake, próximamente el de Room)
// poner que sea ViewModel() permite que la pantalla se adapte y que no se reinicie de 0
class SesionViewModel(private val usuarioRepository: UsuarioRepository) : ViewModel() {

    //_estado privado pero mutable para que así solo ViewModel escriba
    private val _estado = MutableStateFlow(SesionUiState())
    //Arriba hay un SesionUiState que el que se construyó anteriormente vacío como molde para la pantalla
//estado es público y de solo lectura, así la pantalla lo ve pero no cambia nada
    val estado: StateFlow<SesionUiState> = _estado.asStateFlow()

   //se ejecuta solo una vez e inicia una tarea de segundo plano
    init {
        viewModelScope.launch {      usuarioRepository.usuarioActual().collect { usuario ->
                _estado.update { it.copy(sesionIniciada = usuario != null) } //collect queda escuchando el Flow del repositorio y también si es que la sesion ya estaba iniciada
            }
        }
    }

   //ahora lo que hay en sesionUiEvent se escribirá aquí
    fun onEvent(evento: SesionUiEvent) {
        when (evento) {
            is SesionUiEvent.OnCorreoChange -> _estado.update {
                it.copy(
                    correo = evento.valor,
                    errorCorreo = Validaciones.validarCorreo(evento.valor.trim()),
                    mensajeError = null
                )
            }
            is SesionUiEvent.OnClaveChange -> _estado.update {
                it.copy(
                    clave = evento.valor,
                    errorClave = Validaciones.validarClaveLogin(evento.valor),
                    mensajeError = null
                )
            }
            SesionUiEvent.OnIniciarSesion -> iniciarSesion()
        }
    }

    private fun iniciarSesion() {
        //toma el estado de ese momemto y hace las comparaciones
        val actual = _estado.value
        if (!actual.puedeIniciar) return

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, mensajeError = null) }
            try {
                val usuario = usuarioRepository.iniciarSesion(actual.correo.trim().lowercase(), actual.clave)
                _estado.update {
                    it.copy(
                        cargando = false,
                        mensajeError = if (usuario == null) "Correo o contraseña incorrectos" else null
                    )
                }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(cargando = false, mensajeError = "No se pudo iniciar sesión, intenta de nuevo")
                }
            }
        }
    }

    //Crea los ViewModel pero...
    companion object {
        //con factory le damos el relleno de los constructores vacíos
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                SesionViewModel(app.container.usuarioRepository)
            }
        }
    }
}
