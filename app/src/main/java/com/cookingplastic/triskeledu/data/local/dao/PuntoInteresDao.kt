package com.cookingplastic.triskeledu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import kotlinx.coroutines.flow.Flow

// Consultas SQL de la tabla puntos_interes. Room genera el código que las ejecuta.
// Las que devuelven Flow vuelven a emitir solas cada vez que la tabla cambia.
@Dao
interface PuntoInteresDao {

    @Query("SELECT * FROM puntos_interes WHERE activo = 1 ORDER BY nombre")
    fun obtenerActivos(): Flow<List<PuntoInteres>>

    @Query("SELECT * FROM puntos_interes ORDER BY nombre")
    fun obtenerTodos(): Flow<List<PuntoInteres>>

    @Query("SELECT * FROM puntos_interes WHERE id = :id")
    fun obtenerPorId(id: String): Flow<PuntoInteres?>

    // Lectura única, para revisar el punto antes de activarlo
    @Query("SELECT * FROM puntos_interes WHERE id = :id")
    suspend fun buscarPorId(id: String): PuntoInteres?

    @Query("SELECT * FROM puntos_interes WHERE codigoQr = :codigo LIMIT 1")
    suspend fun obtenerPorCodigoQr(codigo: String): PuntoInteres?

    // Upsert: inserta si el id no existe; si existe, lo actualiza
    @Upsert
    suspend fun guardar(punto: PuntoInteres)

    @Query("DELETE FROM puntos_interes WHERE id = :id")
    suspend fun eliminar(id: String)

    @Query("UPDATE puntos_interes SET activo = :activo, actualizadoEn = :fecha WHERE id = :id")
    suspend fun cambiarActivo(id: String, activo: Boolean, fecha: Long)

    @Query("SELECT COUNT(*) FROM puntos_interes")
    suspend fun contar(): Int

    // Carga inicial de DatosPrueba. IGNORE deja intactas las filas que ya existen.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTodos(puntos: List<PuntoInteres>)

    // Las cápsulas de prueba se insertan antes que los puntos, porque cada punto
    // las referencia con su clave foránea capsulaId
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarCapsulasDePrueba(capsulas: List<Capsula>)
}
