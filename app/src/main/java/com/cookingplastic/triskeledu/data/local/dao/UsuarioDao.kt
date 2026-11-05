package com.cookingplastic.triskeledu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.RolUsuario
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

    //SIM-01: contraseña nueva, también con hash y sal
    @Query("UPDATE usuarios SET claveHash = :hash, claveSal = :sal WHERE id = :id")
    suspend fun cambiarClave(id: String, hash: String, sal: String)

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun buscarPorId(id: String): Usuario?

    //RN-24: para dejar una notificación a cada usuario que corresponde (por ejemplo, a los administradores)
    @Query("SELECT * FROM usuarios")
    suspend fun todos(): List<Usuario>

    //PER-02: el índice único de username lanza error si ya existe
    @Query("UPDATE usuarios SET username = :username WHERE id = :id")
    suspend fun cambiarUsername(id: String, username: String)

    //ADM-10: las cuentas que esperan respuesta, de la más antigua a la más nueva
    @Query("SELECT * FROM usuarios WHERE solicitudCreador = 'PENDIENTE' ORDER BY solicitadoEn")
    fun solicitudesPendientes(): Flow<List<Usuario>>

    //RN-29: solo cambia si la solicitud sigue PENDIENTE. Devuelve 0 si otro administrador ya la respondió (EXC-09)
    @Query(
        "UPDATE usuarios SET rol = :rol, solicitudCreador = :estado, solicitudResueltaEn = :fecha, solicitudResueltaPor = :adminId " +
            "WHERE id = :id AND solicitudCreador = 'PENDIENTE'"
    )
    suspend fun resolverSolicitud(id: String, rol: RolUsuario, estado: EstadoSolicitud, fecha: Long, adminId: String): Int

    //Upsert: la primera vez crea la fila 1 de sesiones; después la reemplaza
    @Upsert
    suspend fun guardarSesion(sesion: Sesion)

    //JOIN une la sesión con su usuario. Sin sesión, o con usuarioId en null, emite null.
    //Como es Flow, vuelve a emitir cuando cambia cualquiera de las dos tablas
    @Query("SELECT usuarios.* FROM sesiones INNER JOIN usuarios ON usuarios.id = sesiones.usuarioId WHERE sesiones.id = 1")
    fun usuarioActual(): Flow<Usuario?>
}
