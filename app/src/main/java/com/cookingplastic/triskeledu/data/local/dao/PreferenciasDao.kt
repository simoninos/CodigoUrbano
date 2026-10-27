package com.cookingplastic.triskeledu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cookingplastic.triskeledu.data.model.Preferencias
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferenciasDao {

    //null si ese usuario todavía no ha guardado preferencias
    @Query("SELECT * FROM preferencias WHERE usuarioId = :usuarioId")
    fun preferenciasDe(usuarioId: String): Flow<Preferencias?>

    @Upsert
    suspend fun guardar(preferencias: Preferencias)
}
