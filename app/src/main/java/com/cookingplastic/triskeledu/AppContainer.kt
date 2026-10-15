package com.cookingplastic.triskeledu

import android.content.Context
import androidx.room.Room
import com.cookingplastic.triskeledu.data.local.AppDatabase
import com.cookingplastic.triskeledu.data.repository.CapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakePreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.data.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository

//Context para que así room sepa dónde guardar
class AppContainer(context: Context) {

    //Se crea base de datos y con lazy se crea la priimera vez que alguien lo pida, no antes
    val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "triskeledu.db").build()
    }

    //Usaremos las Fake y así se crean los repositorios
    val usuarioRepository: UsuarioRepository = FakeUsuarioRepository()
    val preferenciasRepository: PreferenciasRepository = FakePreferenciasRepository()
    val capsulaRepository: CapsulaRepository = FakeCapsulaRepository()
}
