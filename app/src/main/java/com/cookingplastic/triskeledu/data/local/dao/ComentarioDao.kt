package com.cookingplastic.triskeledu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cookingplastic.triskeledu.data.model.Comentario
import kotlinx.coroutines.flow.Flow

//Consultas de la tabla comentarios
@Dao
interface ComentarioDao {

    @Query("SELECT * FROM comentarios WHERE puntoId = :puntoId ORDER BY creadoEn DESC")
    fun comentariosDe(puntoId: String): Flow<List<Comentario>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(comentario: Comentario)

    //SIM-03: lo que falta enviar al servidor
    @Query("SELECT * FROM comentarios WHERE usuarioId = :usuarioId AND sincronizado = 0 ORDER BY creadoEn")
    suspend fun pendientesDe(usuarioId: String): List<Comentario>

    @Query("UPDATE comentarios SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)

    @Query("DELETE FROM comentarios WHERE id = :id")
    suspend fun quitar(id: String)
}