package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.TipoMonumento

data class MapaUiState(
    // Aqui van a ir los valores correspondiente al MapaUiState
    val cargando: Boolean = true,
    val mensajeError: String? = null
)