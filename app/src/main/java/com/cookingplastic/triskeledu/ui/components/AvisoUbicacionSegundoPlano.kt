package com.cookingplastic.triskeledu.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.outlined.ShareLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cookingplastic.triskeledu.data.location.ServicioLugaresCercanos

//RNF-03 y EXC-13: sin la ubicación "todo el tiempo", "Lugar cercano" funciona solo con la app abierta.
//Explica cómo activarla: con la ubicación normal ya concedida se pide la de segundo plano (en Android 11 o superior
//el sistema abre directamente sus ajustes); si se rechazó, se abren los ajustes de la app
@Composable
fun AvisoUbicacionSegundoPlano(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    var concedido by rememberSaveable { mutableStateOf(ServicioLugaresCercanos.permisoSegundoPlano(contexto)) }
    var ubicacionNormal by rememberSaveable { mutableStateOf(tienePermisoUbicacion(contexto)) }
    var rechazado by rememberSaveable { mutableStateOf(false) }

    //al volver de los ajustes se revisa de nuevo
    val ciclo = LocalLifecycleOwner.current
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                concedido = ServicioLugaresCercanos.permisoSegundoPlano(contexto)
                ubicacionNormal = tienePermisoUbicacion(contexto)
            }
        }
        ciclo.lifecycle.addObserver(observador)
        onDispose { ciclo.lifecycle.removeObserver(observador) }
    }
    val pedirSegundoPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        concedido = ServicioLugaresCercanos.permisoSegundoPlano(contexto)
        if (!ok) rechazado = true
    }
    //sin la ubicación normal, primero se pide esa (Android no deja pedir las dos juntas)
    val activarUbicacion = rememberActivarUbicacion { ubicacionNormal = it }
    if (concedido) return

    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ShareLocation, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Avisos con la app minimizada", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(
                "Ahora \"Lugar cercano\" llega solo con la app abierta. Para recibirlo también con la app minimizada, " +
                    "en Ajustes > Permisos > Ubicación elige \"Permitir todo el tiempo\". Mientras busca, verás una notificación fija.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            TextButton(
                onClick = {
                    when {
                        !ubicacionNormal -> activarUbicacion(EstadoUbicacion.SIN_PERMISO)
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !rechazado ->
                            pedirSegundoPlano.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        else -> try {
                            contexto.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
                            )
                        } catch (e: Exception) {
                            //sin pantalla de ajustes, el texto ya explica cómo hacerlo
                        }
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    when {
                        !ubicacionNormal -> "Activar ubicación"
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !rechazado -> "Permitir todo el tiempo"
                        else -> "Abrir ajustes de la app"
                    }
                )
            }
        }
    }
}
