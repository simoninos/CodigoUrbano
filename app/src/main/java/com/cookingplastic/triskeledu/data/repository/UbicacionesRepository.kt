package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.Ubicacion
import kotlinx.coroutines.flow.Flow

class UbicacionesRepository {
    fun ubicaciones(): Flow<Ubicacion>{
        while (true) {
            val nuevaPosicion = Ubicacion(-33.446944, -70.657500, 555.0)
        }


    }
}