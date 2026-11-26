package com.cookingplastic.triskeledu.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.LecturaGps
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Contrato de la ubicación del usuario.
interface UbicacionRepository {

    // Emite la posición del usuario cada vez que cambia.
    // Sin permiso de ubicación, el Flow termina con SecurityException.
    fun ubicaciones(): Flow<Ubicacion>

    // La última posición conocida, o null si todavía no hay ninguna
    suspend fun ultimaUbicacion(): Ubicacion?

    // SEG-01: la última posición con lo que dice el GPS de ella (si es simulada y su precisión), para escanear
    suspend fun lecturaParaEscaneo(): LecturaGps? = ultimaUbicacion()?.let { LecturaGps(it) }

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

// Ubicación real con Fused Location Provider (Google Play Services), que combina GPS,
// Wi-Fi y antenas para dar la mejor posición con menos batería (U-01).
class GpsUbicacionRepository(private val context: Context) : UbicacionRepository {

    private val cliente = LocationServices.getFusedLocationProviderClient(context)

    // Última posición recibida, para entregarla al instante a la próxima pantalla (U-08)
    private val ultima = MutableStateFlow<Ubicacion?>(null)
    // SEG-01: la misma, con la marca de simulada y la precisión
    private val ultimaLectura = MutableStateFlow<LecturaGps?>(null)

    private fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    // callbackFlow convierte los avisos de LocationCallback en un Flow. Las lecturas empiezan
    // cuando alguien recolecta el Flow y se detienen cuando deja de hacerlo (U-07).
    @SuppressLint("MissingPermission") // el permiso se revisa en tienePermiso()
    override fun ubicaciones(): Flow<Ubicacion> = callbackFlow {
        if (!tienePermiso()) {
            close(SecurityException("Sin permiso de ubicación"))
            awaitClose { }
            return@callbackFlow
        }

        val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                val location = resultado.lastLocation ?: return
                val ubicacion = location.aUbicacion()
                ultima.value = ubicacion
                ultimaLectura.value = location.aLectura()
                trySend(ubicacion)
            }
        }

        ultima.value?.let { trySend(it) }

        try {
            cliente.requestLocationUpdates(solicitud, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose { cliente.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    override suspend fun ultimaUbicacion(): Ubicacion? {
        ultima.value?.let { return it }
        if (!tienePermiso()) return null
        // lastLocation es una Task de Play Services: se espera su resultado sin bloquear el hilo
        return suspendCancellableCoroutine { continuacion ->
            try {
                cliente.lastLocation
                    .addOnSuccessListener { location -> continuacion.resume(location?.aUbicacion()) }
                    .addOnFailureListener { continuacion.resume(null) }
            } catch (e: SecurityException) {
                continuacion.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun lecturaParaEscaneo(): LecturaGps? {
        ultimaLectura.value?.let { return it }
        if (!tienePermiso()) return null
        return suspendCancellableCoroutine { continuacion ->
            try {
                cliente.lastLocation
                    .addOnSuccessListener { location -> continuacion.resume(location?.aLectura()) }
                    .addOnFailureListener { continuacion.resume(null) }
            } catch (e: SecurityException) {
                continuacion.resume(null)
            }
        }
    }

    // SEG-01: Android marca la posición que entrega una app de "ubicación de prueba" (opciones de desarrollador)
    private fun Location.aLectura(): LecturaGps = LecturaGps(
        ubicacion = aUbicacion(),
        simulada = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else @Suppress("DEPRECATION") isFromMockProvider,
        precision = if (hasAccuracy()) accuracy.toDouble() else null
    )

    // U-02: si el teléfono no informa altitud, se usa 0
    private fun Location.aUbicacion(): Ubicacion = Ubicacion(
        latitud = latitude,
        longitud = longitude,
        altitud = if (hasAltitude()) altitude else 0.0
    )
}

// Versión de prueba: una posición fija a 35 m de La Moneda.
// moverA() cambia la posición para simular que el usuario camina.
class FakeUbicacionRepository(
    inicial: Ubicacion = DatosPrueba.UBICACION_CERCA_LA_MONEDA
) : UbicacionRepository {

    private val actual = MutableStateFlow(inicial)

    // SEG-01: para probar el bloqueo con una app de GPS falso
    var simulada = false

    override suspend fun lecturaParaEscaneo(): LecturaGps = LecturaGps(actual.value, simulada, 8.0)

    override fun ubicaciones(): Flow<Ubicacion> = actual

    override suspend fun ultimaUbicacion(): Ubicacion? = actual.value

    fun moverA(ubicacion: Ubicacion) {
        actual.value = ubicacion
    }
}
