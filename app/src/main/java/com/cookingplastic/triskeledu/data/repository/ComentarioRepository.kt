package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.Comentario
import kotlinx.coroutines.flow.Flow

//Contrato de los comentarios: lo usa Detalle (Simon) para guardarlos y listarlos
interface ComentarioRepository {
    //comentarios de un lugar, del más nuevo al más antiguo
    fun comentariosDe(puntoId: String): Flow<List<Comentario>>

    suspend fun agregar(comentario: Comentario)
}