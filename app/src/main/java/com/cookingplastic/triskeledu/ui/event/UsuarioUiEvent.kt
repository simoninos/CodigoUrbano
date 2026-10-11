package com.cookingplastic.triskeledu.ui.event

sealed interface UsuarioUiEvent{
    // Data class para representar acciones de edición (actualizan el estado en memoria)
    data class onPuntosChange(val valor: Int) : UsuarioUiEvent
    data class onMonumentosEscaneadosChange(val valor: Int) : UsuarioUiEvent

    // Data Object para representar acciones de negocio (se disparán al ralizar acciones asincronas)
    data object onCargarUsuario : UsuarioUiEvent
    data object OnGuardarUsuario : UsuarioUiEvent
}