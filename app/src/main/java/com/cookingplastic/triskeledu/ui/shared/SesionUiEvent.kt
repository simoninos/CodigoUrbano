package com.cookingplastic.triskeledu.ui.shared

//aquí se dan los "avisos" (eventos) para avisarle a ViewModel
sealed interface SesionUiEvent {
    data class OnCorreoChange(val valor: String) : SesionUiEvent
    data class OnClaveChange(val valor: String) : SesionUiEvent
    //data object sirve porque es un aviso que no necesita nada
    data object OnIniciarSesion : SesionUiEvent
    //ACC-08: rellena el correo y la contraseña de prueba; no inicia sesión
    data class OnAccesoRapido(val correo: String) : SesionUiEvent
    //la pantalla ya abrió P23
    data object OnSolicitudPendienteVista : SesionUiEvent
}
