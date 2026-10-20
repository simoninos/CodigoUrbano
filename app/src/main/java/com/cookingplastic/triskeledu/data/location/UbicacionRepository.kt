package com.cookingplastic.triskeledu.data.location

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.Ubicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Contrato de la ubicación del usuario.
interface UbicacionRepository {

    // Emite la posición del usuario cada vez que cambia
    fun ubicaciones(): Flow<Ubicacion>

    // La última posición conocida, o null si todavía no hay ninguna
    suspend fun ultimaUbicacion(): Ubicacion?

    // Distancia en metros entre dos ubicaciones, sin considerar la altitud
    fun distanciaMetros(a: Ubicacion, b: Ubicacion): Double = distanciaEnMetros(a, b)
}

private const val RADIO_TIERRA_METROS = 6_371_000.0

// Fórmula de haversine: distancia sobre la superficie de la Tierra entre dos coordenadas.
// Es una función de nivel superior para poder usarla sin crear un repositorio.
fun distanciaEnMetros(a: Ubicacion, b: Ubicacion): Double {
    val lat1 = Math.toRadians(a.latitud)
    val lat2 = Math.toRadians(b.latitud)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(b.longitud - a.longitud)
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * RADIO_TIERRA_METROS * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

// Versión de prueba: una posición fija a 35 m de La Moneda.
// moverA() cambia la posición para simular que el usuario camina.
class FakeUbicacionRepository(
    inicial: Ubicacion = DatosPrueba.UBICACION_CERCA_LA_MONEDA
) : UbicacionRepository {

    private val actual = MutableStateFlow(inicial)

    override fun ubicaciones(): Flow<Ubicacion> = actual

    override suspend fun ultimaUbicacion(): Ubicacion? = actual.value

    fun moverA(ubicacion: Ubicacion) {
        actual.value = ubicacion
    }
}
