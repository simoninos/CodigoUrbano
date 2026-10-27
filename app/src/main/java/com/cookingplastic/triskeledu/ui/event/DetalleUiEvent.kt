package com.cookingplastic.triskeledu.ui.event

// Acciones del usuario en el detalle de un punto
sealed interface DetalleUiEvent {
    data object OnVerVideo : DetalleUiEvent
    data object OnTomarSelfie : DetalleUiEvent
    // Abre o cierra el formulario de comentario
    data object OnComentar : DetalleUiEvent
    data class OnTextoComentarioChange(val texto: String) : DetalleUiEvent
    data object OnPublicarComentario : DetalleUiEvent
    data object OnCerrarSinConexion : DetalleUiEvent
    data object OnMensajeMostrado : DetalleUiEvent
    // El enlace "Descubre más" no se pudo abrir en el teléfono
    data object OnEnlaceNoDisponible : DetalleUiEvent
}
