package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.dao.ComentarioDao
import com.cookingplastic.triskeledu.data.model.Comentario
import kotlinx.coroutines.flow.Flow

//Versión con Room de los comentarios
class RoomComentarioRepository(
    private val dao: ComentarioDao
) : ComentarioRepository {

    override fun comentariosDe(puntoId: String): Flow<List<Comentario>> = dao.comentariosDe(puntoId)

    override suspend fun agregar(comentario: Comentario) {
        dao.insertar(comentario)
    }

    override suspend fun pendientesDe(usuarioId: String): List<Comentario> = dao.pendientesDe(usuarioId)

    override suspend fun marcarSincronizados(ids: List<String>) {
        if (ids.isNotEmpty()) dao.marcarSincronizados(ids)
    }

    override suspend fun quitar(id: String) {
        dao.quitar(id)
    }
}