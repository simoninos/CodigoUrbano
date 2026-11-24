package com.cookingplastic.triskeledu.data.simulacion

import com.cookingplastic.triskeledu.data.local.dao.NotificacionDao
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.notificaciones.AvisoLocal
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

//Cómo terminó "Aprobar" o "Rechazar" en P22
enum class ResultadoSolicitud {
    RESUELTA,
    //EXC-09: otro administrador ya la había respondido
    YA_RESPONDIDA
}

//SIM-02: solicitudes de creador sin backend. Cuando exista el servidor se cambia por una versión con Retrofit
interface SolicitudesRepository {
    fun pendientes(): Flow<List<Usuario>>
    suspend fun aprobar(usuarioId: String, adminId: String): ResultadoSolicitud
    suspend fun rechazar(usuarioId: String, adminId: String): ResultadoSolicitud
    //RN-29: al iniciar sesión, la app consulta si su solicitud fue respondida y muestra el aviso
    suspend fun avisarRespuesta(usuarioId: String)
}

//Versión local con Room, en el mismo teléfono: el administrador resuelve en P22 y el usuario
//recibe la notificación al volver a iniciar sesión
class SolicitudesSimuladas(
    private val usuarioRepository: UsuarioRepository,
    private val notificacionDao: NotificacionDao,
    private val avisoLocal: AvisoLocal,
    private val ahora: () -> Long = System::currentTimeMillis
) : SolicitudesRepository {

    override fun pendientes(): Flow<List<Usuario>> = usuarioRepository.solicitudesPendientes()

    override suspend fun aprobar(usuarioId: String, adminId: String) = resolver(usuarioId, aprobar = true, adminId = adminId)

    override suspend fun rechazar(usuarioId: String, adminId: String) = resolver(usuarioId, aprobar = false, adminId = adminId)

    private suspend fun resolver(usuarioId: String, aprobar: Boolean, adminId: String): ResultadoSolicitud {
        //null: ya no estaba pendiente
        val resuelto = usuarioRepository.resolverSolicitud(usuarioId, aprobar, adminId, ahora())
            ?: return ResultadoSolicitud.YA_RESPONDIDA
        //queda en el historial del usuario (P16); la verá al volver a entrar
        notificacionDao.insertar(
            Notificacion(
                usuarioId = resuelto.id,
                tipo = TipoNotificacion.SOLICITUD,
                titulo = if (aprobar) TITULO_APROBADA else TITULO_RECHAZADA,
                texto = if (aprobar) "Ya puedes crear cápsulas desde Configuración › Mis cápsulas."
                else "Tu cuenta quedó como cliente: puedes seguir explorando y sumando puntos.",
                creadaEn = ahora()
            )
        )
        return ResultadoSolicitud.RESUELTA
    }

    //muestra en el sistema las respuestas que todavía no lee en el historial
    override suspend fun avisarRespuesta(usuarioId: String) {
        notificacionDao.notificacionesDe(usuarioId).first()
            .filter { it.tipo == TipoNotificacion.SOLICITUD && !it.leida }
            .forEach { avisoLocal.mostrar(it.tipo, it.titulo, it.texto) }
    }

    companion object {
        const val TITULO_APROBADA = "¡Ya eres creador de contenido!"
        const val TITULO_RECHAZADA = "Tu solicitud de creador no fue aprobada"
    }
}
