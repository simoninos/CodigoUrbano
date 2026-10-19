package com.cookingplastic.triskeledu.ui.event

//aquí se dan los "avisos" (eventos) para avisarle a ViewModel
sealed interface SesionUiEvent {
    data class OnCorreoChange(val valor: String) : SesionUiEvent
    data class OnClaveChange(val valor: String) : SesionUiEvent
    //data object sirve porque es un aviso que no necesita nada
    data object OnIniciarSesion : SesionUiEvent
}
