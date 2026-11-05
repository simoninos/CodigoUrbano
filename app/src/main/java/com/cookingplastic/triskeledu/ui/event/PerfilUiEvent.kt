package com.cookingplastic.triskeledu.ui.event

sealed interface PerfilUiEvent {
    data class OnUsernameChange(val valor: String) : PerfilUiEvent
    data class OnClaveActualChange(val valor: String) : PerfilUiEvent
    data class OnClaveNuevaChange(val valor: String) : PerfilUiEvent
    data class OnRepetirClaveChange(val valor: String) : PerfilUiEvent
    data object OnGuardar : PerfilUiEvent
    data object OnMensajeMostrado : PerfilUiEvent
}
