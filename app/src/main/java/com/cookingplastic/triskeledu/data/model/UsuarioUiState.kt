package com.cookingplastic.triskeledu.data.model

data class UsuarioUiState(
    val usuario: Usuario = Usuario("", 0, 0),
    val cargando: Boolean = true,
    val mensajeError: String? = ""
)