package com.cookingplastic.triskeledu.ui.screens.mapa

import com.cookingplastic.triskeledu.data.model.TipoMonumento




data class TarjetaUsuarioUiState(
    val usuario: String,
    val puntos: Int
)

data class UbicacionUsuarioUiState(
    val latitud: Double,
    val longitud: Double,
    val zoom: Double
)

data class UbicacionEdificioUiState(
    val latitud: Double,
    val longitud: Double,
    val tipoEdificio: TipoMonumento
)