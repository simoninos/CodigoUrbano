package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.security.HashContrasena
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository

//Usaremos un clase fake para rellenar datos por el momento
//Parte con los mismos usuarios de DatosPrueba (clave de todos: Demo1234) y guarda la clave con hash, como Room
class FakeUsuarioRepository(usuariosIniciales: List<Usuario> = DatosPrueba.usuarios) : UsuarioRepository {

    //en un StateFlow para que la lista de solicitudes (P22) se actualice sola
    private val usuarios = MutableStateFlow(usuariosIniciales)

    //Es una sesión simulada
    private val sesion = MutableStateFlow<Usuario?>(null)

    //Ahora vienen las funciones..
    override suspend fun registrar(username: String, correo: String, clave: String, version: String, quiereSerCreador: Boolean): Usuario {
        val hash = HashContrasena.hashear(clave)
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
        usuarios.update { it + nuevo }
        return nuevo
    }

    override suspend fun iniciarSesion(correo: String, clave: String): Usuario? {
        val encontrado = usuarios.value.find { it.correo == correo } ?: return null
        if (!HashContrasena.verificar(clave, encontrado.claveHash, encontrado.claveSal)) return null
        //igual que Room: la cuenta en revisión no inicia sesión (RN-29)
        if (encontrado.solicitudCreador == EstadoSolicitud.PENDIENTE) return encontrado
        sesion.value = encontrado
        return encontrado
    }

    //Este devuelve un Flow para observarla, pero no cambiar el valor
    override fun usuarioActual(): Flow<Usuario?> = sesion

    //Este cambia el estado
    override suspend fun cerrarSesion() {
        sesion.value = null
    }

    override suspend fun existeUsername(username: String): Boolean = usuarios.value.any { it.username == username }

    override suspend fun existeCorreo(correo: String): Boolean = usuarios.value.any { it.correo == correo }

    override fun solicitudesPendientes(): Flow<List<Usuario>> =
        usuarios.map { lista -> lista.filter { it.solicitudCreador == EstadoSolicitud.PENDIENTE }.sortedBy { it.solicitadoEn } }

    override suspend fun resolverSolicitud(usuarioId: String, aprobar: Boolean, adminId: String, fecha: Long): Usuario? {
        val usuario = usuarios.value.find { it.id == usuarioId } ?: return null
        //igual que Room: solo se resuelve si sigue pendiente (EXC-09)
        if (usuario.solicitudCreador != EstadoSolicitud.PENDIENTE) return null
        val resuelto = usuario.copy(
            rol = if (aprobar) RolUsuario.CREADOR else RolUsuario.CLIENTE,
            solicitudCreador = if (aprobar) EstadoSolicitud.APROBADA else EstadoSolicitud.RECHAZADA,
            solicitudResueltaEn = fecha,
            solicitudResueltaPor = adminId
        )
        usuarios.update { lista -> lista.map { if (it.id == usuarioId) resuelto else it } }
        return resuelto
    }

    override suspend fun todos(): List<Usuario> = usuarios.value

    override suspend fun cambiarUsername(usuarioId: String, username: String): Boolean {
        if (usuarios.value.any { it.username == username && it.id != usuarioId }) return false
        actualizar(usuarioId) { it.copy(username = username) }
        return true
    }

    override suspend fun cambiarClave(usuarioId: String, actual: String, nueva: String): Boolean {
        val usuario = usuarios.value.find { it.id == usuarioId } ?: return false
        if (!HashContrasena.verificar(actual, usuario.claveHash, usuario.claveSal)) return false
        val hash = HashContrasena.hashear(nueva)
        actualizar(usuarioId) { it.copy(claveHash = hash.hash, claveSal = hash.sal) }
        return true
    }

    //cambia el usuario en la lista y, si es el conectado, también en la sesión (como el JOIN de Room)
    private fun actualizar(usuarioId: String, cambio: (Usuario) -> Usuario) {
        usuarios.update { lista -> lista.map { if (it.id == usuarioId) cambio(it) else it } }
        sesion.value?.takeIf { it.id == usuarioId }?.let { sesion.value = cambio(it) }
    }
}
