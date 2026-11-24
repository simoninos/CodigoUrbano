package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.dao.ProgresoDao
import com.cookingplastic.triskeledu.data.model.PUNTOS_CAPSULA
import com.cookingplastic.triskeledu.data.model.PUNTOS_SELFIE
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.cookingplastic.triskeledu.domain.repository.ProgresoRepository

//Versión con Room: el progreso queda guardado en SQLite y sigue ahí al cerrar la app.
//Implementa la misma interfaz que la Fake, así que las pantallas no cambian al usarla
class RoomProgresoRepository(
    private val dao: ProgresoDao
) : ProgresoRepository {

    override fun progresoDe(usuarioId: String): Flow<List<ProgresoPunto>> = dao.progresoDe(usuarioId)

    override fun progresoDePunto(usuarioId: String, puntoId: String): Flow<ProgresoPunto?> =
        dao.progresoDePunto(usuarioId, puntoId)

    override suspend fun registrarEscaneo(usuarioId: String, puntoId: String, ubicacion: Ubicacion): ProgresoPunto {
        //si ya lo escaneó antes, no se crea otra fila ni se suman otros 10 puntos
        dao.buscar(usuarioId, puntoId)?.let { return it }

        val nueva = ProgresoPunto(usuarioId = usuarioId, puntoId = puntoId, escaneoUbicacion = ubicacion)
        dao.insertar(nueva)
        return nueva
    }

    override fun totalPuntos(usuarioId: String): Flow<Int> = dao.totalPuntos(usuarioId)

    override suspend fun registrarVistaCapsula(usuarioId: String, puntoId: String): Boolean =
        dao.registrarVistaCapsula(usuarioId, puntoId, System.currentTimeMillis(), PUNTOS_CAPSULA) == 1

    override fun vistasCompletas(puntoId: String): Flow<Int> = dao.vistasCompletas(puntoId, PUNTOS_CAPSULA)

    override fun escaneosPorPunto(): Flow<Map<String, Int>> =
        dao.escaneosPorPunto().map { filas -> filas.associate { it.puntoId to it.total } }

    override suspend fun registrarSelfie(usuarioId: String, puntoId: String, selfieUri: String): Boolean =
        dao.registrarSelfie(usuarioId, puntoId, selfieUri, System.currentTimeMillis(), PUNTOS_SELFIE) == 1

    override suspend fun pendientesDe(usuarioId: String): List<ProgresoPunto> = dao.pendientesDe(usuarioId)

    override suspend fun marcarSincronizados(ids: List<String>) {
        if (ids.isNotEmpty()) dao.marcarSincronizados(ids)
    }

    override suspend fun quitar(id: String) {
        dao.quitar(id)
    }

    override fun todas(): Flow<List<ProgresoPunto>> = dao.todas()
}
