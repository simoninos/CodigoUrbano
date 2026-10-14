package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.PuntoInteres

data class MonumentoUiState(
    val monumentos: List<PuntoInteres> = emptyList(),
    val cargando: Boolean = false,
    val mensajeError: String? = null
) {
    val totalMonumentos: Int
        get() = monumentos.size
}