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
    //null cuando se elige "Sin cápsula"
    data class OnCapsulaChange(val capsulaId: String?) : AdminEditarUiEvent
    data class OnActivoChange(val valor: Boolean) : AdminEditarUiEvent
    data object OnUsarMiUbicacion : AdminEditarUiEvent
    //P19 (ADM-03): abrir el selector, cancelarlo o usar el lugar elegido
    data object OnAbrirSelectorUbicacion : AdminEditarUiEvent
    data object OnCerrarSelectorUbicacion : AdminEditarUiEvent
    data class OnUbicacionElegida(val latitud: Double, val longitud: Double) : AdminEditarUiEvent
    //ADM-08: dónde quedó el PNG del QR, o null si no se pudo guardar
    data class OnQrDescargado(val dondeQuedo: String?) : AdminEditarUiEvent
    //la pantalla avisa si el usuario negó un permiso, y el ViewModel decide el mensaje
    data object OnPermisoCamaraDenegado : AdminEditarUiEvent
    data object OnPermisoUbicacionDenegado : AdminEditarUiEvent
    data object OnGuardar : AdminEditarUiEvent
}
