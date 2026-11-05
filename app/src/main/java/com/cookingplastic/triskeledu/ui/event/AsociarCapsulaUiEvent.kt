package com.cookingplastic.triskeledu.ui.event

sealed interface AsociarCapsulaUiEvent {
    data class OnElegir(val capsulaId: String) : AsociarCapsulaUiEvent
    data object OnAsociar : AsociarCapsulaUiEvent
}
