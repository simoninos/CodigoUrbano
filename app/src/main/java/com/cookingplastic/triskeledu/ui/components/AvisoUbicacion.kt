package com.cookingplastic.triskeledu.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

//EXC-01: cómo está la ubicación del teléfono
enum class EstadoUbicacion {
    LISTA,
    SIN_PERMISO,
    GPS_APAGADO
}

fun tienePermisoUbicacion(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

fun gpsEncendido(context: Context): Boolean {
    val gestor = context.getSystemService(LocationManager::class.java) ?: return false
    return LocationManagerCompat.isLocationEnabled(gestor)
}

fun estadoUbicacion(context: Context): EstadoUbicacion = when {
    !tienePermisoUbicacion(context) -> EstadoUbicacion.SIN_PERMISO
    !gpsEncendido(context) -> EstadoUbicacion.GPS_APAGADO
    else -> EstadoUbicacion.LISTA
}

//Cuántas veces seguidas se negó el permiso. Después de dos, Android ya no muestra el diálogo y hay que ir a Ajustes
private const val ARCHIVO_PERMISOS = "permisos"
private const val NEGACIONES_UBICACION = "negaciones_ubicacion"

fun registrarRespuestaUbicacion(context: Context, concedido: Boolean) {
    val preferencias = context.getSharedPreferences(ARCHIVO_PERMISOS, Context.MODE_PRIVATE)
    preferencias.edit { putInt(NEGACIONES_UBICACION, if (concedido) 0 else preferencias.getInt(NEGACIONES_UBICACION, 0) + 1) }
}

private fun negadoDosVeces(context: Context): Boolean =
    context.getSharedPreferences(ARCHIVO_PERMISOS, Context.MODE_PRIVATE).getInt(NEGACIONES_UBICACION, 0) >= 2

//El estado se vuelve a revisar cada vez que la pantalla vuelve al frente (por ejemplo, desde Ajustes)
@Composable
fun rememberEstadoUbicacion(): EstadoUbicacion {
    val context = LocalContext.current
    var estado by remember { mutableStateOf(estadoUbicacion(context)) }
    val ciclo = LocalLifecycleOwner.current
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) estado = estadoUbicacion(context)
        }
        ciclo.lifecycle.addObserver(observador)
        onDispose { ciclo.lifecycle.removeObserver(observador) }
    }
    return estado
}

//La acción del botón del aviso: pide el permiso; si ya se negó dos veces abre los ajustes de la app;
//con el GPS apagado abre los ajustes de ubicación. alResponder recibe si quedó concedido
@Composable
fun rememberActivarUbicacion(alResponder: (Boolean) -> Unit = {}): (EstadoUbicacion) -> Unit {
    val context = LocalContext.current
    val actividad = LocalActivity.current
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { respuestas ->
        val concedido = respuestas.values.any { it }
        registrarRespuestaUbicacion(context, concedido)
        alResponder(concedido)
    }
    return remember(context, actividad, pedir) {
        { estado ->
            when (estado) {
                EstadoUbicacion.GPS_APAGADO -> abrir(context, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                EstadoUbicacion.SIN_PERMISO -> {
                    val sinDialogo = negadoDosVeces(context) && (actividad == null ||
                        !ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.ACCESS_FINE_LOCATION))
                    if (sinDialogo) {
                        abrir(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                    } else {
                        pedir.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }
                EstadoUbicacion.LISTA -> Unit
            }
        }
    }
}

//si el teléfono no tiene esa pantalla de ajustes, no pasa nada (RNF-11)
private fun abrir(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        //sin la pantalla de ajustes el aviso sigue visible
    }
}

//EXC-01: el aviso del mapa, en una tarjeta
@Composable
fun AvisoUbicacion(estado: EstadoUbicacion, alActivar: () -> Unit, modifier: Modifier = Modifier) {
    if (estado == EstadoUbicacion.LISTA) return
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        ContenidoAvisoUbicacion(estado = estado, alActivar = alActivar, modifier = Modifier.padding(16.dp))
    }
}

//El mismo aviso sin tarjeta, para ponerlo dentro de otra (Escanear)
@Composable
fun ContenidoAvisoUbicacion(estado: EstadoUbicacion, alActivar: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Activa la ubicación para encontrar lugares", style = MaterialTheme.typography.titleSmall)
        }
        Text(
            if (estado == EstadoUbicacion.GPS_APAGADO) "El GPS del teléfono está apagado. Sin él no vemos qué lugares tienes cerca ni se puede escanear."
            else "Sin permiso de ubicación no vemos qué lugares tienes cerca ni se puede escanear.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = alActivar, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (estado == EstadoUbicacion.GPS_APAGADO) "Encender el GPS" else "Activar ubicación")
        }
    }
}
