package com.cookingplastic.triskeledu.ui.event

import com.cookingplastic.triskeledu.data.model.CategoriaCapsula

//Lo que hace el creador en P18
sealed interface CapsulaEditarUiEvent {
    data class OnTituloChange(val valor: String) : CapsulaEditarUiEvent
    data class OnDescripcionChange(val valor: String) : CapsulaEditarUiEvent
    data class OnCategoriaChange(val valor: CategoriaCapsula) : CapsulaEditarUiEvent
    data class OnEnlaceChange(val valor: String) : CapsulaEditarUiEvent
    data class OnDuracionChange(val valor: String) : CapsulaEditarUiEvent
    data object OnGuardar : CapsulaEditarUiEvent
    data object OnProbarVideo : CapsulaEditarUiEvent
    data object OnEliminar : CapsulaEditarUiEvent
    data object OnConfirmar : CapsulaEditarUiEvent
    data object OnCancelarConfirmacion : CapsulaEditarUiEvent
    data object OnVistaPreviaAbierta : CapsulaEditarUiEvent
}
