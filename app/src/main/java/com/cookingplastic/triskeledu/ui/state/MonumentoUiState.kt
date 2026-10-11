package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.Monumento

data class MonumentoUiState(
    val monumentos: List<Monumento> = emptyList(),
    val cargando: Boolean = false,
    val mensajeError: String? = null
) {
    val totalMonumentos: Int
        get() = monumentos.size
}