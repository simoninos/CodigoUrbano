package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Sesion
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.security.HashContrasena
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

//Versión con Room: usuario y sesión quedan guardados al cerrar la app. La clave se guarda solo como hash
class RoomUsuarioRepository(private val dao: UsuarioDao) : UsuarioRepository {

    override suspend fun registrar(username: String, correo: String, clave: String, version: String, quiereSerCreador: Boolean): Usuario {
        //el hash tarda: Dispatchers.Default lo calcula fuera del hilo principal para que la pantalla no se trabe
        val hash = withContext(Dispatchers.Default) { HashContrasena.hashear(clave) }
        val nuevo = Usuario(
            username = username,
            correo = correo,
            claveHash = hash.hash,
            claveSal = hash.sal,
            terminosVersion = version,
            terminosAceptadosEn = System.currentTimeMillis(),
            solicitudCreador = if (quiereSerCreador) EstadoSolicitud.PENDIENTE else null,
            solicitadoEn = if (quiereSerCreador) System.currentTimeMillis() else null
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
        //cuenta en revisión: no entra a la app (RN-29); la pantalla muestra P23
        if (usuario.solicitudCreador == EstadoSolicitud.PENDIENTE) return usuario
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

    override fun solicitudesPendientes(): Flow<List<Usuario>> = dao.solicitudesPendientes()

    override suspend fun resolverSolicitud(usuarioId: String, aprobar: Boolean, adminId: String, fecha: Long): Usuario? {
        val cambiadas = dao.resolverSolicitud(
            id = usuarioId,
            rol = if (aprobar) RolUsuario.CREADOR else RolUsuario.CLIENTE,
            estado = if (aprobar) EstadoSolicitud.APROBADA else EstadoSolicitud.RECHAZADA,
            fecha = fecha,
            adminId = adminId
        )
        return if (cambiadas == 1) dao.buscarPorId(usuarioId) else null
    }

    override suspend fun todos(): List<Usuario> = dao.todos()

    override suspend fun cambiarUsername(usuarioId: String, username: String): Boolean {
        val usado = dao.existeUsername(username) && dao.buscarPorId(usuarioId)?.username != username
        if (usado) return false
        dao.cambiarUsername(usuarioId, username)
        return true
    }

    override suspend fun cambiarClave(usuarioId: String, actual: String, nueva: String): Boolean {
        val usuario = dao.buscarPorId(usuarioId) ?: return false
        val correcta = withContext(Dispatchers.Default) { HashContrasena.verificar(actual, usuario.claveHash, usuario.claveSal) }
        if (!correcta) return false
        val hash = withContext(Dispatchers.Default) { HashContrasena.hashear(nueva) }
        dao.cambiarClave(usuarioId, hash.hash, hash.sal)
        return true
    }
}
