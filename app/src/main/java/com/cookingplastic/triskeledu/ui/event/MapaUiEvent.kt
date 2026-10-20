package com.cookingplastic.triskeledu.ui.event

// Acciones del usuario en la pantalla del Mapa
sealed interface MapaUiEvent {
    data class OnClickPunto(val puntoId: String) : MapaUiEvent
    data object OnCerrarTarjeta : MapaUiEvent
    data object OnCentrar : MapaUiEvent
    data object OnErrorMostrado : MapaUiEvent
}
