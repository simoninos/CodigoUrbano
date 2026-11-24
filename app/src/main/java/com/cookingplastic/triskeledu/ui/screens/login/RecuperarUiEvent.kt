package com.cookingplastic.triskeledu.ui.screens.login

sealed interface RecuperarUiEvent {
    data class OnCorreoChange(val valor: String) : RecuperarUiEvent
    data class OnCodigoChange(val valor: String) : RecuperarUiEvent
    data class OnClaveNuevaChange(val valor: String) : RecuperarUiEvent
    data class OnRepetirClaveChange(val valor: String) : RecuperarUiEvent
    data object OnEnviarCodigo : RecuperarUiEvent
    data object OnReenviarCodigo : RecuperarUiEvent
    //la pantalla ya preguntó por el permiso de notificaciones; concedido = se podrá ver el código
    data class OnPermisoRespondido(val concedido: Boolean) : RecuperarUiEvent
    data object OnCambiarClave : RecuperarUiEvent
}
