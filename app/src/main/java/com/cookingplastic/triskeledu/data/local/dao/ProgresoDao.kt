package com.cookingplastic.triskeledu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import kotlinx.coroutines.flow.Flow

//Consultas de la tabla progreso_puntos. Las que devuelven Flow vuelven a emitir solas cuando la tabla cambia
@Dao
interface ProgresoDao {

    @Query("SELECT * FROM progreso_puntos WHERE usuarioId = :usuarioId ORDER BY escaneadoEn DESC")
    fun progresoDe(usuarioId: String): Flow<List<ProgresoPunto>>

    @Query("SELECT * FROM progreso_puntos WHERE usuarioId = :usuarioId AND puntoId = :puntoId LIMIT 1")
    fun progresoDePunto(usuarioId: String, puntoId: String): Flow<ProgresoPunto?>

    //lectura única, para revisar si ya existe antes de insertar
    @Query("SELECT * FROM progreso_puntos WHERE usuarioId = :usuarioId AND puntoId = :puntoId LIMIT 1")
    suspend fun buscar(usuarioId: String, puntoId: String): ProgresoPunto?

    //IGNORE: si ya hay una fila de ese usuario y lugar (índice único), no se inserta otra
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(progreso: ProgresoPunto)

    //COALESCE devuelve 0 cuando el usuario todavía no tiene filas
    @Query(
        "SELECT COALESCE(SUM(puntosEscaneo + puntosVideo + puntosSelfie), 0) " +
                "FROM progreso_puntos WHERE usuarioId = :usuarioId"
    )
    fun totalPuntos(usuarioId: String): Flow<Int>
}