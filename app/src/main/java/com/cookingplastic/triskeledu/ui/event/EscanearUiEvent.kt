package com.cookingplastic.triskeledu.ui.event

//Lo que puede pasar en la pantalla Escanear
sealed interface EscanearUiEvent {
    //el sistema respondió si hay permiso de cámara
    data class OnPermisoCamara(val concedido: Boolean) : EscanearUiEvent

    //el sistema respondió si hay permiso de ubicación
    data class OnPermisoUbicacion(val concedido: Boolean) : EscanearUiEvent

    //ML Kit encontró un QR y entrega su texto
    data class OnCodigoLeido(val codigo: String) : EscanearUiEvent

    //el usuario quiere leer otro código
    data object OnEscanearOtro : EscanearUiEvent
}