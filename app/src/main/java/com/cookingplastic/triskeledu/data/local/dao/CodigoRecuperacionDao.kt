package com.cookingplastic.triskeledu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cookingplastic.triskeledu.data.model.CodigoRecuperacion

//Códigos de recuperación simulados (SIM-01): uno por usuario
@Dao
interface CodigoRecuperacionDao {

    //Upsert: un código nuevo reemplaza al anterior del mismo usuario
    @Upsert
    suspend fun guardar(codigo: CodigoRecuperacion)

    @Query("SELECT * FROM codigos_recuperacion WHERE usuarioId = :usuarioId")
    suspend fun buscar(usuarioId: String): CodigoRecuperacion?

    @Query("UPDATE codigos_recuperacion SET usado = 1 WHERE usuarioId = :usuarioId")
    suspend fun marcarUsado(usuarioId: String)
}
