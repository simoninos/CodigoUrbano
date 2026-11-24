package com.cookingplastic.triskeledu.ui.screens.creador

//"Publicar" lleva lo que confirmó el reproductor: si el video cargó y cuánto dura de verdad
sealed interface CapsulaPreviaUiEvent {
    data class OnPublicar(val videoCargado: Boolean, val duracionMs: Long) : CapsulaPreviaUiEvent
}
