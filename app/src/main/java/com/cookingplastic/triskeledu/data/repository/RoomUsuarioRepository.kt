package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.Sesion
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.security.HashContrasena
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

//Versión con Room: usuario y sesión quedan guardados al cerrar la app. La clave se guarda solo como hash
class RoomUsuarioRepository(private val dao: UsuarioDao) : UsuarioRepository {

    override suspend fun registrar(username: String, correo: String, clave: String, version: String): Usuario {
        //el hash tarda: Dispatchers.Default lo calcula fuera del hilo principal para que la pantalla no se trabe
        val hash = withContext(Dispatchers.Default) { HashContrasena.hashear(clave) }
        val nuevo = Usuario(
            username = username,
            correo = correo,
            claveHash = hash.hash,
            claveSal = hash.sal,
            terminosVersion = version,
            terminosAceptadosEn = System.currentTimeMillis()
        )
        dao.insertar(nuevo)
        return nuevo
    }

    override suspend fun iniciarSesion(correo: String, clave: String): Usuario? {
        val usuario = dao.buscarPorCorreo(correo) ?: return null
        val claveCorrecta = withContext(Dispatchers.Default) {
            HashContrasena.verificar(clave, usuario.claveHash, usuario.claveSal)
        }
        if (!claveCorrecta) return null
        dao.guardarSesion(Sesion(usuarioId = usuario.id))
        return usuario
    }

    override fun usuarioActual(): Flow<Usuario?> = dao.usuarioActual()

    //no se borra la fila: queda en null, que significa "nadie conectado"
    override suspend fun cerrarSesion() {
        dao.guardarSesion(Sesion(usuarioId = null))
    }

    override suspend fun existeUsername(username: String): Boolean = dao.existeUsername(username)

    override suspend fun existeCorreo(correo: String): Boolean = dao.existeCorreo(correo)
}
