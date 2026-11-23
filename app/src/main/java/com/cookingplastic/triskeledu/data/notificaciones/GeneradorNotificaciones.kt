package com.cookingplastic.triskeledu.data.notificaciones

import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.repository.NotificacionRepository
import com.cookingplastic.triskeledu.data.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import kotlin.math.roundToInt

//PER-04 y RN-24: arma las notificaciones locales. Cada una queda en el historial (P16) aunque se descarte del
//sistema, y respeta los interruptores de Configuración de quien la recibe. En el sistema (AvisoLocal) solo se
//muestra si la recibe el usuario conectado: en esta simulación todos usan el mismo teléfono
class GeneradorNotificaciones(
    private val notificaciones: NotificacionRepository,
    private val avisoLocal: AvisoLocal,
    private val usuarioRepository: UsuarioRepository,
    private val preferenciasRepository: PreferenciasRepository,
    private val ahora: () -> Long = System::currentTimeMillis,
    private val zona: ZoneId = ZoneId.systemDefault()
) {
    //dos lecturas de GPS casi juntas no deben dejar dos avisos del mismo lugar
    private val turno = Mutex()

    //"Lugar cercano": a 200 m o menos de un punto activo sin escanear, como máximo una vez al día por punto
    suspend fun lugarCercano(usuarioId: String, punto: PuntoInteres, distanciaMetros: Double) = turno.withLock {
        if (distanciaMetros > ReglaAvisos.RADIO_CERCANO_METROS) return@withLock
        val historial = notificaciones.notificacionesDe(usuarioId).first()
        if (ReglaAvisos.yaAvisadoHoy(historial, punto.id, ahora(), zona)) return@withLock
        enviar(
            usuarioId,
            TipoNotificacion.LUGAR_CERCANO,
            titulo = "Lugar cercano",
            texto = "Estás a ${distanciaMetros.roundToInt()} m de ${punto.nombre}. ¡Búscalo!",
            puntoId = punto.id
        )
    }

    //"Logro": completó un lugar (80 de 80)
    suspend fun lugarCompletado(usuarioId: String, punto: PuntoInteres) {
        enviar(
            usuarioId,
            TipoNotificacion.LOGRO,
            titulo = "¡Lugar completado!",
            texto = "Terminaste ${punto.nombre}: 80 de 80 puntos.",
            puntoId = punto.id
        )
    }

    //"Nueva cápsula": se activó un punto. Les llega a los clientes y creadores que tengan el aviso encendido
    suspend fun nuevaCapsula(punto: PuntoInteres) {
        usuarioRepository.todos().filter { it.rol != RolUsuario.ADMINISTRADOR }.forEach { usuario ->
            enviar(
                usuario.id,
                TipoNotificacion.NUEVA_CAPSULA,
                titulo = "Nueva cápsula disponible",
                texto = "${punto.nombre} ya tiene su cápsula sobre IA.",
                puntoId = punto.id,
                capsulaId = punto.capsulaId
            )
        }
    }

    //Al administrador: alguien pidió ser creador (ACC-07)
    suspend fun nuevaSolicitud(solicitante: Usuario) {
        administradores().forEach { admin ->
            enviar(
                admin.id,
                TipoNotificacion.SOLICITUD,
                titulo = "Nueva solicitud de creador",
                texto = "${solicitante.username} pidió ser creador de contenido."
            )
        }
    }

    //Al administrador: un creador asoció una cápsula y el punto quedó Inactivo, listo para activarlo (RN-28)
    suspend fun puntoListo(punto: PuntoInteres, creador: Usuario) {
        administradores().forEach { admin ->
            enviar(
                admin.id,
                TipoNotificacion.PUNTO_LISTO,
                titulo = "Punto listo para activar",
                texto = "${creador.username} asignó una cápsula a ${punto.nombre}. Está Inactivo.",
                puntoId = punto.id,
                capsulaId = punto.capsulaId
            )
        }
    }

    //EXC-08: el servidor no aceptó un escaneo hecho sin conexión y se quitaron sus puntos
    suspend fun visitaRechazada(usuarioId: String, punto: PuntoInteres?, puntoId: String, puntosQuitados: Int, motivo: String) {
        enviar(
            usuarioId,
            TipoNotificacion.SISTEMA,
            titulo = "No pudimos validar tu visita a ${punto?.nombre ?: "un lugar"}",
            texto = "$motivo. Quitamos sus $puntosQuitados puntos; el resto de tu progreso se mantiene.",
            puntoId = puntoId.takeIf { punto != null }
        )
    }

    //el servidor no aceptó un comentario escrito sin conexión
    suspend fun comentarioRechazado(usuarioId: String, punto: PuntoInteres?, motivo: String) {
        enviar(
            usuarioId,
            TipoNotificacion.SISTEMA,
            titulo = "No pudimos publicar tu comentario",
            texto = "$motivo${punto?.let { " (${it.nombre})" } ?: ""}. Puedes escribirlo de nuevo.",
            puntoId = punto?.id
        )
    }

    private suspend fun administradores(): List<Usuario> =
        usuarioRepository.todos().filter { it.rol == RolUsuario.ADMINISTRADOR }

    private suspend fun enviar(
        usuarioId: String,
        tipo: TipoNotificacion,
        titulo: String,
        texto: String,
        puntoId: String? = null,
        capsulaId: String? = null
    ) {
        //el interruptor apagado significa que no quiere este aviso: ni en el sistema ni en el historial
        if (!ReglaAvisos.permitido(tipo, preferenciasRepository.preferenciasDe(usuarioId))) return
        notificaciones.insertar(
            Notificacion(usuarioId = usuarioId, tipo = tipo, titulo = titulo, texto = texto, puntoId = puntoId, capsulaId = capsulaId, creadaEn = ahora())
        )
        //EXC-03: sin permiso AvisoLocal no muestra nada, pero ya quedó en el historial
        if (usuarioRepository.usuarioActual().first()?.id == usuarioId) avisoLocal.mostrar(tipo, titulo, texto)
    }
}
