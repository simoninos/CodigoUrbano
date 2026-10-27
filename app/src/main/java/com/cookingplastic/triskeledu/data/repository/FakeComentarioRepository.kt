package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.Comentario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

//Versión de prueba: guarda los comentarios en memoria hasta tener Room
class FakeComentarioRepository(comentariosIniciales: List<Comentario> = emptyList()) : ComentarioRepository {

    private val comentarios = MutableStateFlow(comentariosIniciales)

    override fun comentariosDe(puntoId: String): Flow<List<Comentario>> =
        comentarios.map { lista ->
            lista.filter { it.puntoId == puntoId }.sortedByDescending { it.creadoEn }
        }

    override suspend fun agregar(comentario: Comentario) {
        comentarios.update { it + comentario }
    }
}