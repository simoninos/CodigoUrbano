package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.dao.ProgresoDao
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import kotlinx.coroutines.flow.Flow

//Versión con Room: el progreso queda guardado en SQLite y sigue ahí al cerrar la app.
//Implementa la misma interfaz que la Fake, así que las pantallas no cambian al usarla
class RoomProgresoRepository(
    private val dao: ProgresoDao
) : ProgresoRepository {

    override fun progresoDe(usuarioId: String): Flow<List<ProgresoPunto>> = dao.progresoDe(usuarioId)

    override fun progresoDePunto(usuarioId: String, puntoId: String): Flow<ProgresoPunto?> =
        dao.progresoDePunto(usuarioId, puntoId)

    override suspend fun registrarEscaneo(usuarioId: String, puntoId: String, ubicacion: Ubicacion): ProgresoPunto {
        //si ya lo escaneó antes, no se crea otra fila ni se suman otros 5 puntos
        dao.buscar(usuarioId, puntoId)?.let { return it }

        val nueva = ProgresoPunto(usuarioId = usuarioId, puntoId = puntoId, escaneoUbicacion = ubicacion)
        dao.insertar(nueva)
        return nueva
    }

    override fun totalPuntos(usuarioId: String): Flow<Int> = dao.totalPuntos(usuarioId)
}