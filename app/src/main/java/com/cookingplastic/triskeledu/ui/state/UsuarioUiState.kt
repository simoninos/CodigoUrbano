package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.Usuario

data class UsuarioUiState(
    val usuario: Usuario = Usuario("", 0, 0),
    val cargando: Boolean = true,
    val mensajeError: String? = null
)