package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.Sesion
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import com.cookingplastic.triskeledu.data.security.HashContrasena
import com.cookingplastic.triskeledu.data.security.ClaveDispositivo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

//BKD-02: registro e inicio de sesión contra ms-usuarios. Envuelve al repositorio de Room (mismo patrón que
//PuntosConAvisos): el servidor decide si la cuenta existe y firma el token; Room guarda una copia del usuario y la
//sesión con su token, para que la app siga funcionando sin conexión (offline-first).
//El resto de las operaciones (perfil, solicitudes locales) siguen yendo a Room
class UsuariosConBackend(
    private val local: UsuarioRepository,
    private val dao: UsuarioDao,
    private val api: BackendApi,
    private val nuevoId: () -> String = { UUID.randomUUID().toString() },
    //SEG-05: crea la clave del teléfono para Key Attestation; null en las pruebas que no la usan
    private val clave: ClaveDispositivo? = null
) : UsuarioRepository by local {

    //ms-usuarios revisa que el correo y el nombre estén libres (409 con el campo) y guarda la clave con BCrypt.
    //Sin servidor no se crea la cuenta: si se creara solo en el teléfono, después no podría iniciar sesión en él
    override suspend fun registrar(username: String, correo: String, clave: String, version: String, quiereSerCreador: Boolean): Usuario {
        val sesion = llamar {
            api.registrar(RegistroDto(nuevoId(), username, correo, clave, aceptaTerminos = true, terminosVersion = version, quiereSerCreador = quiereSerCreador))
        }
        return guardarCopia(sesion.usuario, clave)
    }

    //401: correo o contraseña incorrectos (null, como antes). Cuenta en revisión: devuelve el usuario sin sesión (RN-29).
    //Sin conexión (EXC-06): no se entra. Solo el servidor decide si la cuenta existe y sigue habilitada; la copia de
    //Room no basta. La sesión que ya estaba abierta en el teléfono se mantiene aunque se pierda la conexión
    override suspend fun iniciarSesion(correo: String, clave: String): Usuario? {
        val sesion = try {
            val (desafio, atestacion) = atestar()
            llamar { api.iniciarSesion(LoginDto(correo, clave, desafio, atestacion)) }
        } catch (e: ErrorBackend) {
            if (e.codigo == 401) return null
            throw e
        } catch (e: SinServidor) {
            throw SinServidor(e, "No hay conexión con el servidor. Necesitas conexión para iniciar sesión")
        }
        val usuario = guardarCopia(sesion.usuario, clave)
        if (sesion.cuentaEnRevision || usuario.solicitudCreador == EstadoSolicitud.PENDIENTE || sesion.token == null) return usuario
        dao.guardarSesion(Sesion(usuarioId = usuario.id, token = sesion.token))
        return usuario
    }

    //SEG-05: pide un desafío y crea la clave del teléfono atada a él. Si el servidor no da desafío (versión antigua)
    //o el teléfono no puede crear la clave, se entra igual sin atestación; ms-progreso decide según INTEGRIDAD_MODO
    private suspend fun atestar(): Pair<String?, List<String>?> {
        val generador = clave ?: return null to null
        val desafio = try {
            llamar { api.desafio() }.desafio
        } catch (e: ErrorBackend) {
            return null to null
        }
        val cadena = withContext(Dispatchers.Default) { generador.crear(desafio) }
        return desafio to cadena
    }

    //la copia local guarda su propio hash (PBKDF2) de la contraseña, para el inicio de sesión sin conexión
    private suspend fun guardarCopia(dto: UsuarioDto, clave: String): Usuario {
        val hash = withContext(Dispatchers.Default) { HashContrasena.hashear(clave) }
        val usuario = dto.aUsuario(hash.hash, hash.sal)
        dao.guardarUsuario(usuario)
        return usuario
    }
}
