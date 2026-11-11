package com.cookingplastic.triskeledu.data.sincronizacion

import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.domain.repository.ComentarioRepository
import com.cookingplastic.triskeledu.domain.repository.ProgresoRepository

//ESC-02 y BKD-06: después de guardar algo en Room se pide una sincronización. Si no hay red, queda esperando.
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

class ComentariosConSincronizacion(
    private val comentarios: ComentarioRepository,
    private val programar: () -> Unit
) : ComentarioRepository by comentarios {

    override suspend fun agregar(comentario: Comentario) {
        comentarios.agregar(comentario)
        programar()
    }
}
