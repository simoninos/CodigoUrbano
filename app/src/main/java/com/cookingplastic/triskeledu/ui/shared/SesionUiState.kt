package com.cookingplastic.triskeledu.ui.shared

import com.cookingplastic.triskeledu.data.model.RolUsuario

//Empezamos a iniciarlizar lo que de verá en inicio de sesion
data class SesionUiState(
    //Van vacíos para simular la pantalla recién abierta
    val correo: String = "",
    val clave: String = "",
    val errorCorreo: String? = null,
    val errorClave: String? = null,
    val mensajeError: String? = null,
    val cargando: Boolean = false,
    val sesionIniciada: Boolean = false,
    //false hasta que se sabe si había sesión guardada: así la bienvenida no aparece un instante antes del mapa
    val sesionComprobada: Boolean = false,
    //RN-29: la cuenta tiene la solicitud de creador pendiente; se navega a P23 en vez del mapa
    val solicitudPendiente: Boolean = false,
    //RN-18: cambia en cada toque con errores, para llevar el foco al primero
    val intentoEnvio: Int = 0,
    val campoConError: CampoSesion? = null,
    //ACC-08: acceso rápido para la demo
    val usuariosDemo: List<UsuarioDemo> = emptyList()
) {
    //usar get permite que se vata calculando, así se actualiza siempre
    //ya no deshabilita el botón: solo decide si se ve atenuado (RN-18)
    val puedeIniciar: Boolean
        get() = correo.isNotBlank() && clave.isNotBlank() &&
                errorCorreo == null && errorClave == null && !cargando
}

enum class CampoSesion { CORREO, CLAVE }

//Una fila del acceso rápido: nivel null para el administrador y para la cuenta en revisión
data class UsuarioDemo(
    val username: String,
    val correo: String,
    val rol: RolUsuario,
    val nivel: Int?,
    val enRevision: Boolean
) {
    //"camila_explora · Nvl 3" o "pendiente_demo · En revisión"
    val etiqueta: String
        get() = when {
            enRevision -> "$username · En revisión"
            nivel != null -> "$username · Nvl $nivel"
            else -> username
        }
}
