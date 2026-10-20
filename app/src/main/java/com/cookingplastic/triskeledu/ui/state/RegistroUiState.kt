package com.cookingplastic.triskeledu.ui.state

//Igual que SesionUiState: todo vacío, como la pantalla recién abierta
data class RegistroUiState(
    val username: String = "",
    val correo: String = "",
    val clave: String = "",
    val repetirClave: String = "",
    //es una casilla, por eso Boolean. Parte sin marcar porque los términos se aceptan a propósito
    val aceptaTerminos: Boolean = false,
    //un error por cada una de las 5 reglas
    val errorUsername: String? = null,
    val errorCorreo: String? = null,
    val errorClave: String? = null,
    val errorRepetirClave: String? = null,
    val errorTerminos: String? = null,
    //solo para fallos inesperados. Nombre o correo repetido van en el error de su campo
    val mensajeError: String? = null,
    val cargando: Boolean = false,
    //como sesionIniciada: cuando pasa a true, la pantalla navega al mapa
    val registrado: Boolean = false
) {
    //en las claves va isNotEmpty porque los espacios cuentan como caracteres
    //aceptaTerminos está aquí: sin la casilla marcada el botón queda gris
    val puedeRegistrar: Boolean
        get() = username.isNotBlank() && correo.isNotBlank() &&
                clave.isNotEmpty() && repetirClave.isNotEmpty() && aceptaTerminos &&
                errorUsername == null && errorCorreo == null && errorClave == null &&
                errorRepetirClave == null && !cargando
}
