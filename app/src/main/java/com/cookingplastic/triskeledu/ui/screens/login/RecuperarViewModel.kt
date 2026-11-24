package com.cookingplastic.triskeledu.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.simulacion.RecuperacionRepository
import com.cookingplastic.triskeledu.domain.rules.Validaciones
import com.cookingplastic.triskeledu.domain.rules.primerCampoConError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//P02b: recuperar la contraseña con un código de 6 dígitos (ACC-05). El código lo "envía" la simulación (SIM-01)
class RecuperarViewModel(private val recuperacionRepository: RecuperacionRepository) : ViewModel() {

    private val _estado = MutableStateFlow(RecuperarUiState())
    val estado: StateFlow<RecuperarUiState> = _estado.asStateFlow()

    fun onEvent(evento: RecuperarUiEvent) {
        when (evento) {
            is RecuperarUiEvent.OnCorreoChange -> _estado.update {
                it.copy(correo = evento.valor, errorCorreo = Validaciones.validarCorreo(evento.valor.trim()))
            }
            //solo números y hasta 6
            is RecuperarUiEvent.OnCodigoChange -> _estado.update {
                it.copy(codigo = evento.valor.filter(Char::isDigit).take(6), errorCodigo = null)
            }
            is RecuperarUiEvent.OnClaveNuevaChange -> _estado.update {
                it.copy(
                    claveNueva = evento.valor,
                    errorClave = Validaciones.validarClaveRegistro(evento.valor),
                    errorRepetirClave = if (it.repetirClave.isEmpty()) null else Validaciones.validarRepetirClave(evento.valor, it.repetirClave)
                )
            }
            is RecuperarUiEvent.OnRepetirClaveChange -> _estado.update {
                it.copy(repetirClave = evento.valor, errorRepetirClave = Validaciones.validarRepetirClave(it.claveNueva, evento.valor))
            }
            RecuperarUiEvent.OnEnviarCodigo -> pedirCodigo()
            RecuperarUiEvent.OnReenviarCodigo -> _estado.update { it.copy(pedirPermiso = true, mensaje = null) }
            is RecuperarUiEvent.OnPermisoRespondido -> enviarCodigo(evento.concedido)
            RecuperarUiEvent.OnCambiarClave -> cambiarClave()
        }
    }

    //RN-18: primero se revisa el correo; si está bien, la pantalla pide el permiso y después se envía
    private fun pedirCodigo() {
        val actual = _estado.value
        if (actual.cargando) return
        val error = Validaciones.validarCorreo(actual.correo.trim())
        if (error != null) {
            _estado.update { it.copy(errorCorreo = error, campoConError = CampoRecuperar.CORREO, intentoEnvio = it.intentoEnvio + 1) }
            return
        }
        _estado.update { it.copy(pedirPermiso = true) }
    }

    private fun enviarCodigo(notificacionesPermitidas: Boolean) {
        val reenvio = _estado.value.codigoEnviado
        _estado.update { it.copy(pedirPermiso = false, cargando = true) }
        viewModelScope.launch {
            try {
                recuperacionRepository.enviarCodigo(_estado.value.correo.trim().lowercase())
                _estado.update {
                    it.copy(
                        cargando = false,
                        codigoEnviado = true,
                        codigo = "",
                        mensaje = when {
                            //EXC-03: sin permiso el código no aparece como notificación
                            !notificacionesPermitidas -> "Activa las notificaciones en Ajustes para ver el código y toca «Reenviar código»."
                            reenvio -> "Te enviamos un código nuevo."
                            else -> null
                        }
                    )
                }
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, mensaje = "No se pudo enviar el código, intenta de nuevo") }
            }
        }
    }

    private fun cambiarClave() {
        val actual = _estado.value
        if (actual.cargando) return
        //RN-18: errores de todos los campos, también de los vacíos
        val errorCodigo = if (actual.codigo.length == 6) null else CODIGO_INVALIDO
        val errorClave = Validaciones.validarClaveRegistro(actual.claveNueva)
        val errorRepetir = if (actual.repetirClave.isEmpty()) "Repite tu contraseña"
            else Validaciones.validarRepetirClave(actual.claveNueva, actual.repetirClave)
        val primero = primerCampoConError(
            CampoRecuperar.CODIGO to errorCodigo,
            CampoRecuperar.CLAVE_NUEVA to errorClave,
            CampoRecuperar.REPETIR_CLAVE to errorRepetir
        )
        if (primero != null) {
            _estado.update {
                it.copy(
                    errorCodigo = errorCodigo,
                    errorClave = errorClave,
                    errorRepetirClave = errorRepetir,
                    campoConError = primero,
                    intentoEnvio = it.intentoEnvio + 1
                )
            }
            return
        }

        _estado.update { it.copy(cargando = true, mensaje = null) }
        viewModelScope.launch {
            try {
                val cambiada = recuperacionRepository.cambiarClave(actual.correo.trim().lowercase(), actual.codigo, actual.claveNueva)
                _estado.update {
                    if (cambiada) it.copy(cargando = false, listo = true)
                    //vencido, usado o distinto: el mismo mensaje (ERS 4)
                    else it.copy(
                        cargando = false,
                        errorCodigo = CODIGO_INVALIDO,
                        campoConError = CampoRecuperar.CODIGO,
                        intentoEnvio = it.intentoEnvio + 1
                    )
                }
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, mensaje = "No se pudo cambiar la contraseña, intenta de nuevo") }
            }
        }
    }

    companion object {
        const val CODIGO_INVALIDO = "El código no es válido o ya venció"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                RecuperarViewModel(app.container.recuperacionRepository)
            }
        }
    }
}
