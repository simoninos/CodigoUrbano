package com.cookingplastic.triskeledu.ui.screens.notificaciones

import com.cookingplastic.triskeledu.data.model.Notificacion

sealed interface NotificacionesUiEvent {
    //la marca leída y abre lo relacionado
    data class OnTocar(val notificacion: Notificacion) : NotificacionesUiEvent
    data object OnMarcarTodasLeidas : NotificacionesUiEvent
    data object OnDestinoAbierto : NotificacionesUiEvent
}
