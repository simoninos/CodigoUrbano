package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.NivelUsuario
import com.cookingplastic.triskeledu.data.model.RolUsuario

//Campos de P15 en el orden de la pantalla (RN-18)
enum class CampoPerfil { USERNAME, CLAVE_ACTUAL, CLAVE_NUEVA, REPETIR }

//P15 · Mi perfil (PER-02)
data class PerfilUiState(
    //lo guardado, para saber si cambió algo
    val usernameGuardado: String = "",
    val correo: String = "",
    val rol: RolUsuario = RolUsuario.CLIENTE,
    val creadoEn: Long = 0,
    //el administrador no tiene nivel (LVL-05)
    val nivel: NivelUsuario? = null,
    val username: String = "",
    val claveActual: String = "",
    val claveNueva: String = "",
    val repetirClave: String = "",
    val errorUsername: String? = null,
    val errorClaveActual: String? = null,
    val errorClaveNueva: String? = null,
    val errorRepetir: String? = null,
    val intentoEnvio: Int = 0,
    val campoConError: CampoPerfil? = null,
    val guardando: Boolean = false,
    //"Cambios guardados" o un error general
    val mensaje: String? = null,
    val cargando: Boolean = true
) {
    val cambiaUsername: Boolean
        get() = username.trim() != usernameGuardado

    //basta con escribir en uno de los tres para que se quiera cambiar la contraseña
    val cambiaClave: Boolean
        get() = claveActual.isNotEmpty() || claveNueva.isNotEmpty() || repetirClave.isNotEmpty()

    val hayCambios: Boolean
        get() = cambiaUsername || cambiaClave
}
