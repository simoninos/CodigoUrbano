package com.cookingplastic.triskeledu.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.cookingplastic.triskeledu.R
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.notificaciones.ReglaAvisos
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

//RNF-03: "Lugar cercano" también con la app minimizada. Es un servicio en primer plano: mientras corre, Android
//muestra su notificación fija ("Buscando lugares cercanos"). Lee el GPS cada 30 s con bajo consumo y usa el mismo
//GeneradorNotificaciones de la app abierta, así que respeta los 200 m, una vez al día por punto y los interruptores
class ServicioLugaresCercanos : Service() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var vigilancia: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
            ServiceCompat.startForeground(this, ID_NOTIFICACION, notificacionFija(), tipo)
        } catch (e: Exception) {
            //Android no dejó iniciarlo (por ejemplo, sin permiso): "Lugar cercano" sigue con la app abierta (EXC-13)
            stopSelf()
            return START_NOT_STICKY
        }
        if (vigilancia == null) vigilancia = alcance.launch { vigilar() }
        //si Android cierra la app, el servicio no vuelve solo: se inicia de nuevo al abrirla
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        alcance.cancel()
        super.onDestroy()
    }

    private suspend fun vigilar() {
        val container = (application as TriskeleduApp).container
        try {
            ubicaciones().collect { ubicacion ->
                val usuario = container.usuarioRepository.usuarioActual().first()
                val preferencias = container.preferenciasRepository.preferencias().first()
                //cerró sesión o apagó un interruptor: el servicio se detiene
                if (usuario == null || !ReglaAvisos.vigilarEnSegundoPlano(usuario, preferencias, permisoSegundoPlano(this))) {
                    stopSelf()
                    return@collect
                }
                val activos = container.puntoInteresRepository.obtenerActivos().first()
                val escaneados = container.progresoRepository.progresoDe(usuario.id).first().map { it.puntoId }.toSet()
                ReglaAvisos.cercanosSinEscanear(activos, escaneados, ubicacion).forEach { (punto, distancia) ->
                    container.generadorNotificaciones.lugarCercano(usuario.id, punto, distancia)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            //sin GPS o sin permiso: se detiene sin cerrar la app (RNF-11)
            stopSelf()
        }
    }

    //GPS con bajo consumo: cada 30 s o al moverse 25 m
    @SuppressLint("MissingPermission")
    private fun ubicaciones(): Flow<Ubicacion> = callbackFlow {
        val cliente = LocationServices.getFusedLocationProviderClient(this@ServicioLugaresCercanos)
        val solicitud = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, INTERVALO_MS)
            .setMinUpdateDistanceMeters(25f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                val l = resultado.lastLocation ?: return
                trySend(Ubicacion(latitud = l.latitude, longitud = l.longitude, altitud = l.altitude))
            }
        }
        cliente.requestLocationUpdates(solicitud, callback, Looper.getMainLooper())
        awaitClose { cliente.removeLocationUpdates(callback) }
    }

    private fun notificacionFija() = run {
        val gestor = getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(
            NotificationChannel(CANAL, "Búsqueda de lugares cercanos", NotificationManager.IMPORTANCE_LOW)
        )
        val abrirApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Buscando lugares cercanos")
            .setContentText("Te avisamos al estar a 200 m de un lugar por descubrir. Puedes apagarlo en Configuración.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Te avisamos al estar a 200 m de un lugar por descubrir. Puedes apagarlo en Configuración."))
            .setContentIntent(abrirApp)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val ID_NOTIFICACION = 1001
        private const val CANAL = "UBICACION_SEGUNDO_PLANO"
        private const val INTERVALO_MS = 30_000L

        //EXC-13: Android 10 o superior pide aparte "Permitir todo el tiempo"; antes bastaba el permiso normal
        fun permisoSegundoPlano(context: Context): Boolean {
            val normal = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
            if (!normal) return false
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        //La app lo inicia mientras está a la vista (Android no deja iniciarlo desde segundo plano) y lo detiene al
        //cerrar sesión o apagar un interruptor. Si Android no lo permite, no pasa nada: sigue con la app abierta
        fun actualizar(context: Context, debeCorrer: Boolean) {
            val intent = Intent(context, ServicioLugaresCercanos::class.java)
            try {
                if (debeCorrer) ContextCompat.startForegroundService(context, intent) else context.stopService(intent)
            } catch (e: Exception) {
                //EXC-13: "Lugar cercano" sigue funcionando con la app abierta
            }
        }
    }
}
