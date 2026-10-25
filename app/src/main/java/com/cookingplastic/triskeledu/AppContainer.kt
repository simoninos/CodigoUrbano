package com.cookingplastic.triskeledu

import android.content.Context
import androidx.room.Room
import com.cookingplastic.triskeledu.data.local.AppDatabase
import com.cookingplastic.triskeledu.data.local.CargaInicial
import com.cookingplastic.triskeledu.data.repository.CapsulaRepository
import com.cookingplastic.triskeledu.data.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.RoomCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.RoomPreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.RoomUsuarioRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository

//Context para que así room sepa dónde guardar
class AppContainer(context: Context) {

    //Se crea base de datos y con lazy se crea la priimera vez que alguien lo pida, no antes
    val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "triskeledu.db")
            //la primera vez que se crea la base, se llena con DatosPrueba
            .addCallback(CargaInicial)
            //mientras desarrollamos: si sube la versión (por ejemplo, cuando Simon y Catalina agreguen
            //sus tablas), la base se borra y se crea de nuevo en vez de cerrar la app
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    //Ahora los repositorios guardan en Room; los Fake quedan para las pruebas.
    //Siguen siendo una sola instancia: todas las pantallas ven la misma sesión
    val usuarioRepository: UsuarioRepository by lazy { RoomUsuarioRepository(database.usuarioDao()) }
    val preferenciasRepository: PreferenciasRepository by lazy {
        RoomPreferenciasRepository(database.preferenciasDao(), database.usuarioDao())
    }
    val capsulaRepository: CapsulaRepository by lazy { RoomCapsulaRepository(database.capsulaDao()) }
}
