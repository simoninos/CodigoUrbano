package com.cookingplastic.triskeledu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cookingplastic.triskeledu.data.local.dao.CapsulaDao
import com.cookingplastic.triskeledu.data.local.dao.CodigoRecuperacionDao
import com.cookingplastic.triskeledu.data.local.dao.ComentarioDao
import com.cookingplastic.triskeledu.data.local.dao.NotificacionDao
import com.cookingplastic.triskeledu.data.local.dao.PreferenciasDao
import com.cookingplastic.triskeledu.data.local.dao.ProgresoDao
import com.cookingplastic.triskeledu.data.local.dao.PuntoInteresDao
import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.CodigoRecuperacion
import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.Sesion
import com.cookingplastic.triskeledu.data.model.Usuario

//Base de datos
@Database(
    //cada entidad es una tabla de la base
    entities = [
        Usuario::class, Preferencias::class, Sesion::class, Capsula::class,
        PuntoInteres::class, ProgresoPunto::class, Comentario::class,
        Notificacion::class, CodigoRecuperacion::class
    ],
    //versión 3 (EP3): cápsulas con creador y estado, solicitudes de creador, puntajes 10/50/20,
    //notificaciones y códigos de recuperación. Si cambia el esquema, la versión debe subir
    version = 3,
    exportSchema = false
)
//Aquí van los Converters debido a los converters, ejemplo los de RolUsuario a texto (Converters.kt)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    //Room escribe el cuerpo de estas funciones en AppDatabase_Impl
    abstract fun usuarioDao(): UsuarioDao
    abstract fun preferenciasDao(): PreferenciasDao
    abstract fun capsulaDao(): CapsulaDao
    abstract fun puntoInteresDao(): PuntoInteresDao
    abstract fun progresoDao(): ProgresoDao
    abstract fun comentarioDao(): ComentarioDao
    abstract fun notificacionDao(): NotificacionDao
    abstract fun codigoRecuperacionDao(): CodigoRecuperacionDao
}
