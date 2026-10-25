package com.cookingplastic.triskeledu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.cookingplastic.triskeledu.data.model.Sesion
import com.cookingplastic.triskeledu.data.model.Usuario
import kotlinx.coroutines.flow.Flow

//Consultas de usuarios y de la sesión (la sesión siempre apunta a un usuario)
@Dao
interface UsuarioDao {

    //ABORT: si el correo o el username ya existen (índices únicos), lanza error en vez de reemplazar
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: Usuario)

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun buscarPorCorreo(correo: String): Usuario?

    @Query("SELECT EXISTS(SELECT 1 FROM usuarios WHERE username = :username)")
    suspend fun existeUsername(username: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM usuarios WHERE correo = :correo)")
    suspend fun existeCorreo(correo: String): Boolean

    //Upsert: la primera vez crea la fila 1 de sesiones; después la reemplaza
    @Upsert
    suspend fun guardarSesion(sesion: Sesion)

    //JOIN une la sesión con su usuario. Sin sesión, o con usuarioId en null, emite null.
    //Como es Flow, vuelve a emitir cuando cambia cualquiera de las dos tablas
    @Query("SELECT usuarios.* FROM sesiones INNER JOIN usuarios ON usuarios.id = sesiones.usuarioId WHERE sesiones.id = 1")
    fun usuarioActual(): Flow<Usuario?>
}
