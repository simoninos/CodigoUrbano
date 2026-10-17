package com.cookingplastic.triskeledu.ui.event

import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres

//los avisos del formulario del administrador
sealed interface AdminEditarUiEvent {
    data class OnNombreChange(val valor: String) : AdminEditarUiEvent
    data class OnTipoChange(val valor: TipoPuntoInteres) : AdminEditarUiEvent
    data class OnDescripcionChange(val valor: String) : AdminEditarUiEvent
    data class OnLatitudChange(val valor: String) : AdminEditarUiEvent
    data class OnLongitudChange(val valor: String) : AdminEditarUiEvent
    data class OnAltitudChange(val valor: String) : AdminEditarUiEvent
    data class OnDescubreMasChange(val valor: String) : AdminEditarUiEvent
    //la foto que devolvió el Photo Picker
    data class OnImagenElegida(val uri: String) : AdminEditarUiEvent
    data object OnQuitarImagen : AdminEditarUiEvent
    //null cuando se elige "Sin video"
    data class OnCapsulaChange(val capsulaId: String?) : AdminEditarUiEvent
    data class OnActivoChange(val valor: Boolean) : AdminEditarUiEvent
    data object OnGenerarQr : AdminEditarUiEvent
    data object OnGuardar : AdminEditarUiEvent
}
