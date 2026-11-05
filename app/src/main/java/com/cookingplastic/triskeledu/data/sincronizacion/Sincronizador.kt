package com.cookingplastic.triskeledu.data.sincronizacion

import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.puntosTotales
import com.cookingplastic.triskeledu.data.notificaciones.GeneradorNotificaciones
import com.cookingplastic.triskeledu.data.repository.ComentarioRepository
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

//Lo que pasó en una sincronización; sirve para las pruebas
data class ResumenSincronizacion(
    val aceptados: Int = 0,
    val rechazados: Int = 0
)

//SIM-03, ESC-02 y BKD-06: envía al servidor lo que la cuenta conectada guardó con sincronizado = false.
//Lo aceptado pasa a true; lo rechazado se quita del teléfono con un aviso (EXC-08) y el resto se mantiene.
//Si los puntos bajan, el nivel mostrado baja solo (se calcula desde los puntos) y los temas se conservan
//porque nivelMaximoAlcanzado no se toca (EXC-12, RN-33)
class Sincronizador(
    private val servidor: ServidorApi,
    private val progresoRepository: ProgresoRepository,
    private val comentarioRepository: ComentarioRepository,
    private val puntoInteresRepository: PuntoInteresRepository,
    private val usuarioRepository: UsuarioRepository,
    private val avisos: GeneradorNotificaciones
) {
    //dos sincronizaciones juntas podrían enviar dos veces lo mismo
    private val turno = Mutex()

    suspend fun sincronizar(): ResumenSincronizacion = turno.withLock {
        //en el teléfono se envía solo lo de la cuenta con sesión; lo de otras cuentas espera a que entren
        val usuario = usuarioRepository.usuarioActual().first() ?: return@withLock ResumenSincronizacion()
        if (usuario.rol == RolUsuario.ADMINISTRADOR) return@withLock ResumenSincronizacion()
        val escaneos = sincronizarEscaneos(usuario.id)
        val comentarios = sincronizarComentarios(usuario.id)
        ResumenSincronizacion(
            aceptados = escaneos.aceptados + comentarios.aceptados,
            rechazados = escaneos.rechazados + comentarios.rechazados
        )
    }

    private suspend fun sincronizarEscaneos(usuarioId: String): ResumenSincronizacion {
        val pendientes = progresoRepository.pendientesDe(usuarioId)
        if (pendientes.isEmpty()) return ResumenSincronizacion()
        val respuestas = servidor.confirmarEscaneos(usuarioId, pendientes)
        progresoRepository.marcarSincronizados(respuestas.filter { it.aceptado }.map { it.id })
        val rechazadas = respuestas.filterNot { it.aceptado }
        rechazadas.forEach { respuesta ->
            val fila = pendientes.find { it.id == respuesta.id } ?: return@forEach
            //EXC-08: se quitan los puntos de ese lugar (la fila completa) y el usuario recibe el aviso
            progresoRepository.quitar(fila.id)
            val punto = puntoInteresRepository.obtenerPorId(fila.puntoId).first()
            avisos.visitaRechazada(usuarioId, punto, fila.puntoId, fila.puntosTotales(), respuesta.motivo?.explicacion ?: "")
        }
        return ResumenSincronizacion(aceptados = respuestas.size - rechazadas.size, rechazados = rechazadas.size)
    }

    private suspend fun sincronizarComentarios(usuarioId: String): ResumenSincronizacion {
        val pendientes = comentarioRepository.pendientesDe(usuarioId)
        if (pendientes.isEmpty()) return ResumenSincronizacion()
        val respuestas = servidor.confirmarComentarios(usuarioId, pendientes)
        comentarioRepository.marcarSincronizados(respuestas.filter { it.aceptado }.map { it.id })
        val rechazadas = respuestas.filterNot { it.aceptado }
        rechazadas.forEach { respuesta ->
            val comentario = pendientes.find { it.id == respuesta.id } ?: return@forEach
            comentarioRepository.quitar(comentario.id)
            val punto = puntoInteresRepository.obtenerPorId(comentario.puntoId).first()
            avisos.comentarioRechazado(usuarioId, punto, respuesta.motivo?.explicacion ?: "")
        }
        return ResumenSincronizacion(aceptados = respuestas.size - rechazadas.size, rechazados = rechazadas.size)
    }
}
