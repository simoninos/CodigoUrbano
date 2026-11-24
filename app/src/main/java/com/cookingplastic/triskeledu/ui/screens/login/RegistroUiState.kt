package com.cookingplastic.triskeledu.ui.screens.login

//Igual que SesionUiState: todo vacío, como la pantalla recién abierta
data class RegistroUiState(
    val username: String = "",
    val correo: String = "",
    val clave: String = "",
    val repetirClave: String = "",
    //es una casilla, por eso Boolean. Parte sin marcar porque los términos se aceptan a propósito
    val aceptaTerminos: Boolean = false,
    //ACC-07: la cuenta queda en revisión hasta que un administrador responda
    val quiereSerCreador: Boolean = false,
    //un error por cada una de las 5 reglas
    val errorUsername: String? = null,
    val errorCorreo: String? = null,
    val errorClave: String? = null,
    val errorRepetirClave: String? = null,
    val errorTerminos: String? = null,
    //solo para fallos inesperados. Nombre o correo repetido van en el error de su campo
    val mensajeError: String? = null,
    val cargando: Boolean = false,
    //como sesionIniciada: cuando pasa a true, la pantalla navega al mapa (o a P23 si pidió ser creador)
    val registrado: Boolean = false,
    //RN-18: cambia en cada toque con errores, para llevar el foco al primero
    val intentoEnvio: Int = 0,
    val campoConError: CampoRegistro? = null
) {
    //cuántos campos quedaron marcados, para el aviso "Revisa los N campos marcados"
    val camposConError: Int
        get() = listOfNotNull(errorUsername, errorCorreo, errorClave, errorRepetirClave, errorTerminos).size

    //en las claves va isNotEmpty porque los espacios cuentan como caracteres
    //ya no deshabilita el botón: solo decide si se ve atenuado (RN-18)
    val puedeRegistrar: Boolean
        get() = username.isNotBlank() && correo.isNotBlank() &&
                clave.isNotEmpty() && repetirClave.isNotEmpty() && aceptaTerminos &&
                errorUsername == null && errorCorreo == null && errorClave == null &&
                errorRepetirClave == null && !cargando
}

enum class CampoRegistro { USERNAME, CORREO, CLAVE, REPETIR_CLAVE, TERMINOS }
