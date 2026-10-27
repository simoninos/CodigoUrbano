package com.cookingplastic.triskeledu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.cookingplastic.triskeledu.data.model.Capsula
import kotlinx.coroutines.flow.Flow

@Dao
interface CapsulaDao {

    @Query("SELECT * FROM capsulas ORDER BY titulo")
    fun capsulas(): Flow<List<Capsula>>

    @Query("SELECT * FROM capsulas WHERE id = :id")
    suspend fun obtenerPorId(id: String): Capsula?
}
