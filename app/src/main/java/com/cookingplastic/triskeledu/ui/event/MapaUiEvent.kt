package com.cookingplastic.triskeledu.ui.event

import com.cookingplastic.triskeledu.ui.state.CamaraMapa

// Acciones del usuario en la pantalla del Mapa
sealed interface MapaUiEvent {
    data class OnClickPunto(val puntoId: String) : MapaUiEvent
    data object OnCerrarTarjeta : MapaUiEvent
    data object OnCentrar : MapaUiEvent
    // Botón que activa o desactiva el seguimiento de la ubicación (M-20)
    data object OnAlternarSeguimiento : MapaUiEvent
    // El mapa deja la pantalla y entrega dónde quedó la cámara (M-23)
    data class OnCamaraGuardada(val camara: CamaraMapa) : MapaUiEvent
    data object OnErrorMostrado : MapaUiEvent
    // Respuesta del usuario al diálogo de permiso de ubicación
    data class OnPermisoUbicacion(val concedido: Boolean) : MapaUiEvent
}
