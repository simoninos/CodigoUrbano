package com.cookingplastic.triskeledu.ui.state

//Empezamos a iniciarlizar lo que de verá en inicio de sesion
data class SesionUiState(
    //Van vacíos para simular la pantalla recién abierta
    val correo: String = "",
    val clave: String = "",
    val errorCorreo: String? = null,
    val errorClave: String? = null,
    val mensajeError: String? = null,
    val cargando: Boolean = false,
    val sesionIniciada: Boolean = false
) {
    //usar get permite que se vata calculando, así se actualiza siempre
    val puedeIniciar: Boolean
        get() = correo.isNotBlank() && clave.isNotBlank() &&
                errorCorreo == null && errorClave == null && !cargando
}
