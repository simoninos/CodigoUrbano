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

    //solo cambia la fila si la cápsula no se había visto: así los 50 puntos se suman una sola vez.
    //Devuelve cuántas filas cambió (0 o 1). sincronizado vuelve a 0 porque hay algo nuevo que enviar
    @Query(
        "UPDATE progreso_puntos SET capsulaVistaEn = :fecha, puntosCapsula = :puntos, sincronizado = 0 " +
                "WHERE usuarioId = :usuarioId AND puntoId = :puntoId AND capsulaVistaEn IS NULL"
    )
    suspend fun registrarVistaCapsula(usuarioId: String, puntoId: String, fecha: Long, puntos: Int): Int

    //ACT-03: la selfie suma una sola vez por lugar: solo cambia la fila si todavía no tiene selfie
    @Query(
        "UPDATE progreso_puntos SET selfieUri = :uri, selfieEn = :fecha, puntosSelfie = :puntos, sincronizado = 0 " +
                "WHERE usuarioId = :usuarioId AND puntoId = :puntoId AND selfieEn IS NULL"
    )
    suspend fun registrarSelfie(usuarioId: String, puntoId: String, uri: String, fecha: Long, puntos: Int): Int

    //COALESCE devuelve 0 cuando el usuario todavía no tiene filas
    @Query(
        "SELECT COALESCE(SUM(puntosEscaneo + puntosCapsula + puntosSelfie), 0) " +
                "FROM progreso_puntos WHERE usuarioId = :usuarioId"
    )
    fun totalPuntos(usuarioId: String): Flow<Int>

    //CRE-05: filas del punto que ya sumaron los 50 de la cápsula
    @Query("SELECT COUNT(*) FROM progreso_puntos WHERE puntoId = :puntoId AND puntosCapsula = :puntos")
    fun vistasCompletas(puntoId: String, puntos: Int): Flow<Int>

    //P07-Admin y RN-30: escaneos de cada punto, de todos los usuarios
    @Query("SELECT puntoId, COUNT(*) AS total FROM progreso_puntos GROUP BY puntoId")
    fun escaneosPorPunto(): Flow<List<EscaneosPunto>>

    //SIM-03: lo que falta enviar al servidor, del más antiguo al más nuevo
    @Query("SELECT * FROM progreso_puntos WHERE usuarioId = :usuarioId AND sincronizado = 0 ORDER BY escaneadoEn")
    suspend fun pendientesDe(usuarioId: String): List<ProgresoPunto>

    @Query("UPDATE progreso_puntos SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>)

    //EXC-08: escaneo rechazado por el servidor
    @Query("DELETE FROM progreso_puntos WHERE id = :id")
    suspend fun quitar(id: String)

    //LUG-06: todas las filas, para sumar los puntos de cada usuario en el ranking
    @Query("SELECT * FROM progreso_puntos")
    fun todas(): Flow<List<ProgresoPunto>>
}

//Una fila del conteo de escaneos: Room llena los campos por el nombre de las columnas
data class EscaneosPunto(val puntoId: String, val total: Int)
