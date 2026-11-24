package com.cookingplastic.triskeledu.ui.screens.solicitudes

import com.cookingplastic.triskeledu.data.model.Usuario

//Aprobar y Rechazar abren un diálogo; la respuesta se da al confirmar
sealed interface SolicitudesUiEvent {
    data class OnAprobar(val usuario: Usuario) : SolicitudesUiEvent
    data class OnRechazar(val usuario: Usuario) : SolicitudesUiEvent
    data object OnConfirmar : SolicitudesUiEvent
    data object OnCancelar : SolicitudesUiEvent
    data object OnMensajeVisto : SolicitudesUiEvent
}
