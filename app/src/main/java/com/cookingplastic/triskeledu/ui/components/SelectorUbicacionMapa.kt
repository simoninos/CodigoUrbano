package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

//P19 · Selector de ubicación (ADM-03): se abre desde "Elegir en el mapa" en P10. El pin queda en el centro;
//se mueve el mapa (o se toca un lugar) para ubicarlo, y "Usar esta ubicación" entrega latitud y longitud
@Composable
fun SelectorUbicacionMapa(
    latitudInicial: Double,
    longitudInicial: Double,
    alCancelar: () -> Unit,
    alElegir: (latitud: Double, longitud: Double) -> Unit
) {
    var latitud by remember { mutableDoubleStateOf(latitudInicial) }
    var longitud by remember { mutableDoubleStateOf(longitudInicial) }

    Dialog(onDismissRequest = alCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize()) {
            MapaSimple(
                latitud = latitudInicial,
                longitud = longitudInicial,
                zoom = 17.0,
                interactivo = true,
                alMoverCentro = { lat, lon ->
                    latitud = lat
                    longitud = lon
                },
                modifier = Modifier.fillMaxSize()
            )
            //el pin apunta con su punta al centro del mapa
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .offset(y = (-24).dp)
            )
            Surface(
                onClick = alCancelar,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, contentDescription = "Cancelar") }
            }
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.navigationBarsPadding().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Elegir ubicación", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text(
                        "Mueve el mapa o toca un punto para ubicar el pin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Lat ${String.format(Locale.ROOT, "%.5f", latitud)} · Lon ${String.format(Locale.ROOT, "%.5f", longitud)}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = alCancelar, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Cancelar") }
                        Button(onClick = { alElegir(latitud, longitud) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                            Text("Usar esta ubicación")
                        }
                    }
                }
            }
        }
    }
}
