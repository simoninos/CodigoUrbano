package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.Usuario
import kotlinx.coroutines.flow.Flow

//Aquí dejaré las funciones que se usarán
interface UsuarioRepository{
    suspend fun registrar(username: String, correo: String, clave: String, version: String): Usuario
    suspend fun iniciarSesion(correo: String, clave: String): Usuario?
    fun usuarioActual(): Flow<Usuario?>
    suspend fun cerrarSesion()
    suspend fun existeUsername(username: String): Boolean
    suspend fun existeCorreo(correo: String): Boolean
}
