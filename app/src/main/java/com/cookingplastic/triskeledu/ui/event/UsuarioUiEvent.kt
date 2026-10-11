package com.cookingplastic.triskeledu.ui.event

sealed interface UsuarioUiEvent{
    // Data class para representar acciones de edición (actualizan el estado en memoria)
    data class OnPuntosChange(val valor: Int) : UsuarioUiEvent
    data class OnMonumentosEscaneadosChange(val valor: Int) : UsuarioUiEvent

    // Data Object para representar acciones de negocio (se disparán al ralizar acciones asincronas)
    data object OnCargarUsuario : UsuarioUiEvent
    data object OnGuardarUsuario : UsuarioUiEvent
}