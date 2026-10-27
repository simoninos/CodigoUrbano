package com.cookingplastic.triskeledu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cookingplastic.triskeledu.data.local.dao.CapsulaDao
import com.cookingplastic.triskeledu.data.local.dao.ComentarioDao
import com.cookingplastic.triskeledu.data.local.dao.PreferenciasDao
import com.cookingplastic.triskeledu.data.local.dao.ProgresoDao
import com.cookingplastic.triskeledu.data.local.dao.PuntoInteresDao
import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.Comentario
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
        PuntoInteres::class, ProgresoPunto::class, Comentario::class
    ],
    //versión 2: se agregaron puntos, progreso y comentarios. Si cambia el esquema, la versión debe subir
    version = 2,
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
}
