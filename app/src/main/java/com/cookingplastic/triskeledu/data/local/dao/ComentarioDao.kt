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
}