package com.cookingplastic.triskeledu.ui.screens.admin

import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres

// Acciones del administrador en la lista de puntos de interés
sealed interface AdminListaUiEvent {
    data class OnBusquedaChange(val valor: String) : AdminListaUiEvent
    // null = Todos
    data class OnFiltroChange(val filtro: EstadoAdminPunto?) : AdminListaUiEvent
    data class OnCambiarActivo(val puntoId: String, val activo: Boolean) : AdminListaUiEvent
    data class OnPedirEliminar(val punto: PuntoInteres) : AdminListaUiEvent
    data object OnConfirmarEliminar : AdminListaUiEvent
    data object OnCancelarEliminar : AdminListaUiEvent
    data object OnMensajeMostrado : AdminListaUiEvent
}
