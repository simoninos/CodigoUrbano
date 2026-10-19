package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
//Usaremos un clase fake para rellenar datos por el momento
class FakeUsuarioRepository : UsuarioRepository {

    //Vamos a crear usuarios según requerimientos..
    private val usuarios = mutableListOf(
        Usuario(
            username = "cliente_prueba",
            correo = "cliente@triskeledu.cl",
            claveHash = "Cliente123", // TEMPORAL: clave en texto hasta tener HashContrasena
            claveSal = "",
            terminosVersion = "1.0",
            terminosAceptadosEn = System.currentTimeMillis()
        ),
        Usuario(
            username = "admin_prueba",
            correo = "admin@triskeledu.cl",
            claveHash = "Admin1234", // TEMPORAL: clave en texto hasta tener HashContrasena
            claveSal = "",
            rol = RolUsuario.ADMINISTRADOR, //Aquí se pone explícito para que no tome el por defecto
            terminosVersion = "1.0",
            terminosAceptadosEn = System.currentTimeMillis()
        )
    )

    //Es una sesión simulada
    private val sesion = MutableStateFlow<Usuario?>(null)

    //Ahora vienen las funciones..
    override suspend fun registrar(username: String, correo: String, clave: String, version: String): Usuario {
        val nuevo = Usuario(
            username = username,
            correo = correo,
            claveHash = clave, // TEMPORAL: clave en texto hasta tener HashContrasena
            claveSal = "",
            terminosVersion = version,
            terminosAceptadosEn = System.currentTimeMillis()
        )
        usuarios.add(nuevo)
        return nuevo
    }

    override suspend fun iniciarSesion(correo: String, clave: String): Usuario? {
        val encontrado = usuarios.find { it.correo == correo && it.claveHash == clave }
        if (encontrado != null) {
            sesion.value = encontrado
        }
        return encontrado
    }

    //Este devuelve un Flow para observarla, pero no cambiar el valor
    override fun usuarioActual(): Flow<Usuario?> = sesion

    //Este cambia el estado
    override suspend fun cerrarSesion() {
        sesion.value = null
    }

    override suspend fun existeUsername(username: String): Boolean = usuarios.any { it.username == username }

    override suspend fun existeCorreo(correo: String): Boolean = usuarios.any { it.correo == correo }
}
