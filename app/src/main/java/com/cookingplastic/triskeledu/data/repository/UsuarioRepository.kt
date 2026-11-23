package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.Usuario
import kotlinx.coroutines.flow.Flow

//Aquí dejaré las funciones que se usarán
interface UsuarioRepository{
    //quiereSerCreador: la cuenta queda con la solicitud PENDIENTE (ACC-07)
    suspend fun registrar(username: String, correo: String, clave: String, version: String, quiereSerCreador: Boolean = false): Usuario
    //RN-29: con la solicitud de creador pendiente devuelve el usuario, pero no guarda la sesión
    suspend fun iniciarSesion(correo: String, clave: String): Usuario?
    fun usuarioActual(): Flow<Usuario?>
    suspend fun cerrarSesion()
    suspend fun existeUsername(username: String): Boolean
    suspend fun existeCorreo(correo: String): Boolean

    //ADM-10: cuentas con la solicitud de creador PENDIENTE
    fun solicitudesPendientes(): Flow<List<Usuario>>

    //RN-29: aprobar deja la cuenta como CREADOR y rechazar, como CLIENTE. Devuelve el usuario ya resuelto,
    //o null si la solicitud no estaba pendiente (EXC-09)
    suspend fun resolverSolicitud(usuarioId: String, aprobar: Boolean, adminId: String, fecha: Long): Usuario?

    //RN-24: todos los usuarios, para avisar a cada uno lo que le corresponde
    suspend fun todos(): List<Usuario>

    //PER-02: false si el nombre ya lo usa otra persona
    suspend fun cambiarUsername(usuarioId: String, username: String): Boolean

    //PER-02: false si la contraseña actual no es correcta; la nueva se guarda con hash y sal
    suspend fun cambiarClave(usuarioId: String, actual: String, nueva: String): Boolean
}
