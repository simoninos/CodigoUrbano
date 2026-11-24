package com.cookingplastic.triskeledu.ui.screens.creador

//"Retirar publicación" pide confirmación antes de hacerlo
sealed interface CapsulaDetalleUiEvent {
    data object OnRetirar : CapsulaDetalleUiEvent
    data object OnConfirmarRetiro : CapsulaDetalleUiEvent
    data object OnCancelarRetiro : CapsulaDetalleUiEvent
}
