package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.notificaciones.GeneradorNotificaciones
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.event.RegistroUiEvent
import com.cookingplastic.triskeledu.ui.state.CampoRegistro
import com.cookingplastic.triskeledu.ui.state.RegistroUiState
import com.cookingplastic.triskeledu.ui.validation.Validaciones
import com.cookingplastic.triskeledu.ui.validation.primerCampoConError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//Mismo esquema que SesionViewModel, pero sin init: el registro no necesita escuchar la sesión
class RegistroViewModel(
    private val usuarioRepository: UsuarioRepository,
    //RN-24: si pide ser creador, se avisa a los administradores
    private val avisos: GeneradorNotificaciones? = null
) : ViewModel() {

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado: StateFlow<RegistroUiState> = _estado.asStateFlow()

    fun onEvent(evento: RegistroUiEvent) {
        when (evento) {
            is RegistroUiEvent.OnUsernameChange -> _estado.update {
                it.copy(
                    username = evento.valor,
                    errorUsername = Validaciones.validarUsername(evento.valor.trim()),
                    mensajeError = null
                )
            }
            is RegistroUiEvent.OnCorreoChange -> _estado.update {
                it.copy(
                    correo = evento.valor,
                    errorCorreo = Validaciones.validarCorreo(evento.valor.trim()),
                    mensajeError = null
                )
            }
            is RegistroUiEvent.OnClaveChange -> _estado.update {
                it.copy(
                    clave = evento.valor,
                    errorClave = Validaciones.validarClaveRegistro(evento.valor),
                    //si ya escribió la repetida, se vuelve a comparar con la clave nueva
                    errorRepetirClave = if (it.repetirClave.isEmpty()) null
                    else Validaciones.validarRepetirClave(evento.valor, it.repetirClave),
                    mensajeError = null
                )
            }
            is RegistroUiEvent.OnRepetirClaveChange -> _estado.update {
                it.copy(
                    repetirClave = evento.valor,
                    errorRepetirClave = Validaciones.validarRepetirClave(it.clave, evento.valor),
                    mensajeError = null
                )
            }
            //si desmarca la casilla aparece "Debes aceptar los términos"
            is RegistroUiEvent.OnTerminosChange -> _estado.update {
                it.copy(
                    aceptaTerminos = evento.valor,
                    errorTerminos = Validaciones.validarTerminos(evento.valor)
                )
            }
            is RegistroUiEvent.OnQuiereSerCreadorChange -> _estado.update { it.copy(quiereSerCreador = evento.valor) }
            RegistroUiEvent.OnRegistrar -> registrar()
        }
    }

    //RN-18 (ACC-03): calcula el error de cada campo, también de los vacíos que no se han tocado
    private fun revisarTodo(actual: RegistroUiState): RegistroUiState {
        val username = actual.username.trim()
        val errorUsername = if (username.isEmpty()) "El nombre de usuario es obligatorio" else Validaciones.validarUsername(username)
        val errorCorreo = Validaciones.validarCorreo(actual.correo.trim())
        val errorClave = Validaciones.validarClaveRegistro(actual.clave)
        val errorRepetir = if (actual.repetirClave.isEmpty()) "Repite tu contraseña"
            else Validaciones.validarRepetirClave(actual.clave, actual.repetirClave)
        val errorTerminos = Validaciones.validarTerminos(actual.aceptaTerminos)
        return actual.copy(
            errorUsername = errorUsername,
            errorCorreo = errorCorreo,
            errorClave = errorClave,
            errorRepetirClave = errorRepetir,
            errorTerminos = errorTerminos,
            campoConError = primerCampoConError(
                CampoRegistro.USERNAME to errorUsername,
                CampoRegistro.CORREO to errorCorreo,
                CampoRegistro.CLAVE to errorClave,
                CampoRegistro.REPETIR_CLAVE to errorRepetir,
                CampoRegistro.TERMINOS to errorTerminos
            )
        )
    }

    private fun registrar() {
        //se revisa otra vez porque la tecla "Listo" también llama a registrar
        if (_estado.value.cargando) return
        val actual = revisarTodo(_estado.value)
        //con errores: se muestran todos, el foco va al primero y no se crea la cuenta
        if (actual.campoConError != null) {
            _estado.update { actual.copy(intentoEnvio = it.intentoEnvio + 1) }
            return
        }

        //se recortan una vez y se usan iguales en todo el registro
        val username = actual.username.trim()
        //el correo se guarda en minúsculas: así Camila@... y camila@... son la misma cuenta
        val correo = actual.correo.trim().lowercase()

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, mensajeError = null) }
            try {
                //se le pregunta al repositorio porque Validaciones no conoce a los usuarios guardados
                val usernameOcupado = usuarioRepository.existeUsername(username)
                val correoOcupado = usuarioRepository.existeCorreo(correo)

                if (usernameOcupado || correoOcupado) {
                    //el error queda bajo el campo que hay que cambiar; el otro conserva el que tenía
                    _estado.update {
                        it.copy(
                            cargando = false,
                            errorUsername = if (usernameOcupado) "Ese nombre ya está en uso" else it.errorUsername,
                            errorCorreo = if (correoOcupado) "Ese correo ya está registrado" else it.errorCorreo
                        )
                    }
                } else {
                    //la versión de los términos la manda el ViewModel y la fecha la pone el repositorio
                    val nuevo = usuarioRepository.registrar(username, correo, actual.clave, VERSION_TERMINOS, actual.quiereSerCreador)
                    if (actual.quiereSerCreador) runCatching { avisos?.nuevaSolicitud(nuevo) }
                    //se inicia sesión al tiro para que no tenga que escribir todo de nuevo en el Login.
                    //Si pidió ser creador no se inicia: la cuenta queda en revisión (RN-29)
                    if (!actual.quiereSerCreador) usuarioRepository.iniciarSesion(correo, actual.clave)
                    _estado.update { it.copy(cargando = false, registrado = true) }
                }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(cargando = false, mensajeError = "No se pudo crear la cuenta, intenta de nuevo")
                }
            }
        }
    }

    companion object {
        //const val es un valor fijo. Si cambian los términos, se actualiza solo aquí
        //1.1: agrega la ubicación en segundo plano para "Lugar cercano" (RNF-03)
        const val VERSION_TERMINOS = "1.1"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                RegistroViewModel(app.container.usuarioRepository, app.container.generadorNotificaciones)
            }
        }
    }
}
