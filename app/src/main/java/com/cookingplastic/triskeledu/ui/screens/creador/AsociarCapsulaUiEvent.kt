package com.cookingplastic.triskeledu.ui.screens.creador

sealed interface AsociarCapsulaUiEvent {
    data class OnElegir(val capsulaId: String) : AsociarCapsulaUiEvent
    data object OnAsociar : AsociarCapsulaUiEvent
}
