package com.cookingplastic.triskeledu.ui.screens.mislugares

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.data.model.PUNTOS_ESCANEO
import com.cookingplastic.triskeledu.ui.components.fechaCorta

//P07-C (LUG-07): los QR escaneados, con fecha, hora y distancia. Tocar uno abre P04
@Composable
fun ListaQr(qrs: List<QrEscaneado>, alVerDetalle: (String) -> Unit, textoVacio: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (qrs.isEmpty()) TextoVacio(textoVacio)
        qrs.forEach { qr -> TarjetaQr(qr = qr, alPresionar = { alVerDetalle(qr.puntoId) }) }
    }
}

@Composable
private fun TarjetaQr(qr: QrEscaneado, alPresionar: () -> Unit) {
    Surface(
        onClick = alPresionar,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = qr.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Escaneado: ${fechaCorta(qr.escaneadoEn)} · a ${qr.distanciaMetros} m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            EtiquetaPuntos(puntos = PUNTOS_ESCANEO)
        }
    }
}
