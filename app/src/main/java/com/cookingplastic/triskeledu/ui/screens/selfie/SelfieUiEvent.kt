package com.cookingplastic.triskeledu.ui.screens.selfie

//Lo que pasa en P13; la cámara vive en la pantalla y avisa aquí
sealed interface SelfieUiEvent {
    data class OnPermisoCamara(val concedido: Boolean, val bloqueado: Boolean) : SelfieUiEvent
    data object OnCambiarCamara : SelfieUiEvent
    data object OnTomandoFoto : SelfieUiEvent
    //dirección de la foto guardada en el almacenamiento privado de la app
    data class OnFotoGuardada(val uri: String) : SelfieUiEvent
    data object OnErrorCamara : SelfieUiEvent
}
