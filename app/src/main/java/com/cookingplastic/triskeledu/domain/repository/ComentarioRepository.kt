package com.cookingplastic.triskeledu.domain.repository

import com.cookingplastic.triskeledu.data.model.Comentario
import kotlinx.coroutines.flow.Flow

//Contrato de los comentarios: lo usa Detalle (Simon) para guardarlos y listarlos
interface ComentarioRepository {
    //comentarios de un lugar, del más nuevo al más antiguo
    fun comentariosDe(puntoId: String): Flow<List<Comentario>>

    suspend fun agregar(comentario: Comentario)

    //SIM-03: comentarios del usuario que el servidor todavía no confirma
    suspend fun pendientesDe(usuarioId: String): List<Comentario>

    suspend fun marcarSincronizados(ids: List<String>)

    //el servidor rechazó el comentario: se quita del teléfono
    suspend fun quitar(id: String)
}