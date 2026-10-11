package com.cookingplastic.triskeledu.data.model

data class Monumento(
    val id: Int,
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val tipoMonumento: TipoMonumento,
    val descripcion: String = "",
)
