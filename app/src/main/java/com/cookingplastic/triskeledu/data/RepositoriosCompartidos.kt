package com.cookingplastic.triskeledu.data

import android.content.Context
import com.cookingplastic.triskeledu.data.location.GpsUbicacionRepository
import com.cookingplastic.triskeledu.data.location.UbicacionRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository

// Una sola instancia de los repositorios de puntos de interés y de ubicación, compartida por
// todas las pantallas: lo que cambia el administrador se ve en el Mapa y todas usan el mismo GPS.
// Cumple el papel de AppContainer para estos dos repositorios mientras el contenedor no los registre.
object RepositoriosCompartidos {

    val puntoInteres: PuntoInteresRepository by lazy { FakePuntoInteresRepository() }

    @Volatile
    private var gps: UbicacionRepository? = null

    // Se usa el contexto de la aplicación, que vive mientras la app esté abierta
    fun ubicacion(context: Context): UbicacionRepository =
        gps ?: synchronized(this) {
            gps ?: GpsUbicacionRepository(context.applicationContext).also { gps = it }
        }
}
