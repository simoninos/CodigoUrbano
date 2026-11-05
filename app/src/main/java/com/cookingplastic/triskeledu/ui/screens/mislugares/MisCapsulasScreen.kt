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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.data.model.PUNTOS_CAPSULA
import com.cookingplastic.triskeledu.ui.state.CapsulaDescubierta

//P07-B (LUG-04): las cápsulas de los lugares escaneados. Tocar una abre P12, que elige el modo solo (RN-08)
@Composable
fun ListaCapsulas(
    capsulas: List<CapsulaDescubierta>,
    alVerCapsula: (capsulaId: String, puntoId: String) -> Unit,
    textoVacio: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (capsulas.isEmpty()) TextoVacio(textoVacio)
        capsulas.forEach { capsula ->
            TarjetaCapsula(capsula = capsula, alPresionar = { alVerCapsula(capsula.capsulaId, capsula.puntoId) })
        }
    }
}

@Composable
private fun TarjetaCapsula(capsula: CapsulaDescubierta, alPresionar: () -> Unit) {
    Surface(
        onClick = alPresionar,
        shape = RoundedCornerShape(16.dp),
        //las ya vistas se destacan como los lugares completados
        color = if (capsula.vista) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (capsula.vista) 2.dp else 1.dp,
            color = if (capsula.vista) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            //miniatura: un cuadro oscuro con el botón de reproducir, como en el mockup
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.inverseSurface, modifier = Modifier.size(width = 48.dp, height = 64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = capsula.titulo, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Etiqueta(texto = capsula.categoria)
                    Etiqueta(
                        texto = (if (capsula.vista) "Visto" else "Por ver") + " · +$PUNTOS_CAPSULA",
                        fondo = if (capsula.vista) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer,
                        color = if (capsula.vista) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(text = capsula.nombrePunto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
