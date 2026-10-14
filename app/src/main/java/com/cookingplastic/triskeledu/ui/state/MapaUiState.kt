package com.cookingplastic.triskeledu.ui.state

data class MapaUiState(
    // Aqui van los valores correspondiente al MapaUiState
    val ubicacionUsuario: Pair<Double, Double> = Pair(0.0, 0.0),
    val cargando: Boolean = true,
    val mensajeError: String? = null
)