package com.cookingplastic.triskeledu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cookingplastic.triskeledu.data.model.Notificacion
import kotlinx.coroutines.flow.Flow

//Historial de notificaciones (PER-03), de la más nueva a la más antigua
@Dao
interface NotificacionDao {

    @Query("SELECT * FROM notificaciones WHERE usuarioId = :usuarioId ORDER BY creadaEn DESC")
    fun notificacionesDe(usuarioId: String): Flow<List<Notificacion>>

    //contador de la campana
    @Query("SELECT COUNT(*) FROM notificaciones WHERE usuarioId = :usuarioId AND leida = 0")
    fun sinLeer(usuarioId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(notificacion: Notificacion)

    @Query("UPDATE notificaciones SET leida = 1 WHERE id = :id")
    suspend fun marcarLeida(id: String)

    @Query("UPDATE notificaciones SET leida = 1 WHERE usuarioId = :usuarioId")
    suspend fun marcarTodasLeidas(usuarioId: String)
}
