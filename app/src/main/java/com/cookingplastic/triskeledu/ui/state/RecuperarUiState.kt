package com.cookingplastic.triskeledu.ui.state

//P02b Recuperar contraseña (ACC-05, SIM-01): primero el correo; después el código y la contraseña nueva
data class RecuperarUiState(
    val correo: String = "",
    val codigo: String = "",
    val claveNueva: String = "",
    val repetirClave: String = "",
    //true cuando ya se pidió el código: aparecen los campos del paso 2
    val codigoEnviado: Boolean = false,
    val errorCorreo: String? = null,
    val errorCodigo: String? = null,
    val errorClave: String? = null,
    val errorRepetirClave: String? = null,
    //aviso bajo el código ("Te enviamos un código nuevo", falta el permiso de notificaciones...)
    val mensaje: String? = null,
    //antes de enviar el código, la pantalla pide el permiso de notificaciones (Android 13 o superior)
    val pedirPermiso: Boolean = false,
    val cargando: Boolean = false,
    //la contraseña ya se cambió
    val listo: Boolean = false,
    //RN-18: cambia en cada toque con errores, para llevar el foco al primero
    val intentoEnvio: Int = 0,
    val campoConError: CampoRecuperar? = null
) {
    //solo deciden si los botones se ven atenuados (RN-18)
    val puedeEnviarCodigo: Boolean
        get() = correo.isNotBlank() && errorCorreo == null && !cargando

    val puedeCambiarClave: Boolean
        get() = codigo.length == 6 && claveNueva.isNotEmpty() && repetirClave.isNotEmpty() &&
                errorCodigo == null && errorClave == null && errorRepetirClave == null && !cargando
}

enum class CampoRecuperar { CORREO, CODIGO, CLAVE_NUEVA, REPETIR_CLAVE }
