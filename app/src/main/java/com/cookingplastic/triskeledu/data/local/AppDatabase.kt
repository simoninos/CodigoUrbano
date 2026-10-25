package com.cookingplastic.triskeledu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cookingplastic.triskeledu.data.local.dao.CapsulaDao
import com.cookingplastic.triskeledu.data.local.dao.PreferenciasDao
import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.Sesion
import com.cookingplastic.triskeledu.data.model.Usuario

//Base de datos
@Database(
    //En entidades irán porque son tablas, faltan agregar las que correspondan cuando falte (dejo mensaje)
    entities = [Usuario::class, Preferencias::class, Sesion::class, Capsula::class],
    version = 1,
    exportSchema = false
)
//Aquí van los Converters debido a los converters, ejemplo los de RolUsuario a texto (Converters.kt)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    //Room escribe el cuerpo de estas funciones en AppDatabase_Impl
    abstract fun usuarioDao(): UsuarioDao
    abstract fun preferenciasDao(): PreferenciasDao
    abstract fun capsulaDao(): CapsulaDao
}
