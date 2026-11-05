package com.cookingplastic.triskeledu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cookingplastic.triskeledu.data.model.Capsula
import kotlinx.coroutines.flow.Flow

@Dao
interface CapsulaDao {

    @Query("SELECT * FROM capsulas ORDER BY titulo")
    fun capsulas(): Flow<List<Capsula>>

    @Query("SELECT * FROM capsulas WHERE id = :id")
    suspend fun obtenerPorId(id: String): Capsula?

    //la misma cápsula como Flow: P17-B se actualiza al publicarla o retirarla
    @Query("SELECT * FROM capsulas WHERE id = :id")
    fun observar(id: String): Flow<Capsula?>

    //Upsert actualiza la fila sin borrarla, así el punto asociado no pierde su capsulaId
    @Upsert
    suspend fun guardar(capsula: Capsula)

    @Query("DELETE FROM capsulas WHERE id = :id")
    suspend fun eliminar(id: String)
}
