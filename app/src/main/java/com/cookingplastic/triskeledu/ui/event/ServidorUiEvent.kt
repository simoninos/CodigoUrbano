package com.cookingplastic.triskeledu.ui.event

sealed interface ServidorUiEvent {
    data object OnAbrir : ServidorUiEvent
    data object OnCerrar : ServidorUiEvent
    data class OnTextoChange(val texto: String) : ServidorUiEvent
    //valida, guarda y prueba la conexión con el gateway
    data object OnGuardar : ServidorUiEvent
    //vuelve a la dirección de fábrica (la del emulador)
    data object OnRestablecer : ServidorUiEvent
}
