package com.cookingplastic.triskeledu.data.model

data class Usuario(
    val username: String,
    val puntos: Int = 0,
    val monumentosEscaneados: Int = 0,
)