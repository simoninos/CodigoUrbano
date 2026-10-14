package com.cookingplastic.triskeledu.data.model

data class Monumento(
    val id: Int,
    val nombre: String,
    val ubicacion: Pair<Double, Double> = Pair(0.0, 0.0),
    val tipoMonumento: TipoMonumento,
    val descripcion: String = "",
)
