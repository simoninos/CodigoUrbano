package com.cookingplastic.triskeledu.data

import android.content.Context
import com.cookingplastic.triskeledu.data.location.GpsUbicacionRepository
import com.cookingplastic.triskeledu.data.location.UbicacionRepository

// Una sola instancia del GPS para todas las pantallas: así Mapa, Detalle, Escanear y Admin editar
// comparten la última ubicación conocida. Los puntos de interés ya vienen de AppContainer (Room).
object RepositoriosCompartidos {

    @Volatile
    private var gps: UbicacionRepository? = null

    // Se usa el contexto de la aplicación, que vive mientras la app esté abierta
    fun ubicacion(context: Context): UbicacionRepository =
        gps ?: synchronized(this) {
            gps ?: GpsUbicacionRepository(context.applicationContext).also { gps = it }
        }
}
