package com.cookingplastic.triskeledu.ui.screens.login

//P01, "Servidor": la dirección del API Gateway que usa la app (BKD-01)
data class ServidorUiState(
    //la que se usa ahora, para mostrarla bajo "Acceso rápido"
    val actual: String = "",
    val deFabrica: String = "",
    val dialogoAbierto: Boolean = false,
    //lo que se está escribiendo en el diálogo
    val texto: String = "",
    val error: String? = null,
    val probando: Boolean = false,
    //null = no se ha probado; true = el gateway respondió; false = se guardó pero no respondió
    val responde: Boolean? = null
)
