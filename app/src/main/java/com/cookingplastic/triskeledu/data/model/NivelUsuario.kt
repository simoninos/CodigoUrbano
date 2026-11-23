package com.cookingplastic.triskeledu.data.model

//Nivel del usuario (RN-31). No se guarda: lo calcula ReglaNivel desde sus puntos totales
data class NivelUsuario(
    val numero: Int,
    val nombre: String,
    //puntos con que se alcanza este nivel y el siguiente
    val puntosDesde: Int,
    val puntosSiguiente: Int?,
    val puntosTotales: Int
)
