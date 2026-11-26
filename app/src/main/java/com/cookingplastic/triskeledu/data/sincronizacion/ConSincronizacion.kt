package com.cookingplastic.triskeledu.data.sincronizacion

import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.remoto.SinServidor
import com.cookingplastic.triskeledu.domain.repository.ComentarioRepository
import com.cookingplastic.triskeledu.domain.repository.ProgresoRepository

//ESC-02: después de guardar un escaneo en Room se pide una sincronización. Si no hay red, queda esperando.
//"by" delega lo demás sin cambios, igual que ProgresoConNivel
class ProgresoConSincronizacion(
    private val progreso: ProgresoRepository,
    private val programar: () -> Unit
) : ProgresoRepository by progreso {

    override suspend fun registrarEscaneo(usuarioId: String, puntoId: String, ubicacion: Ubicacion): ProgresoPunto =
        progreso.registrarEscaneo(usuarioId, puntoId, ubicacion).also { programar() }

    override suspend fun registrarVistaCapsula(usuarioId: String, puntoId: String): Boolean =
        progreso.registrarVistaCapsula(usuarioId, puntoId).also { if (it) programar() }

    override suspend fun registrarSelfie(usuarioId: String, puntoId: String, selfieUri: String): Boolean =
        progreso.registrarSelfie(usuarioId, puntoId, selfieUri).also { if (it) programar() }
}

//BKD-06: comentar exige conexión. El comentario va primero al servidor, que lo revisa (RN-17); solo si lo acepta
//se guarda en Room, ya sincronizado. Sin servidor no se guarda nada y el texto queda en el formulario
class ComentariosConServidor(
    private val comentarios: ComentarioRepository,
    private val servidor: ServidorApi
) : ComentarioRepository by comentarios {

    override suspend fun agregar(comentario: Comentario) {
        val respuesta = try {
            servidor.confirmarComentarios(comentario.usuarioId, listOf(comentario)).firstOrNull { it.id == comentario.id }
        } catch (e: SinServidor) {
            throw SinServidor(e, "No hay conexión con el servidor. Necesitas conexión para comentar")
        }
        if (respuesta == null || !respuesta.aceptado) {
            throw ComentarioRechazado(respuesta?.motivo?.explicacion ?: "El servidor no aceptó el comentario")
        }
        comentarios.agregar(comentario)
        comentarios.marcarSincronizados(listOf(comentario.id))
    }
}

//El servidor respondió, pero no aceptó el comentario; el mensaje es el motivo
class ComentarioRechazado(motivo: String) : Exception(motivo)
