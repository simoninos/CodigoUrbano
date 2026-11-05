package com.cookingplastic.triskeledu.ui.screens.creador

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.data.model.EstadoCapsula

//Piezas que comparten las pantallas del creador (P17, P17-B, P18 y P21)

//Volver, título y, a la derecha, una etiqueta opcional (por ejemplo el estado de la cápsula)
@Composable
fun CabeceraCreador(titulo: String, alVolver: () -> Unit, modifier: Modifier = Modifier, extra: @Composable () -> Unit = {}) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = alVolver) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        extra()
    }
}

//Miniatura vertical con el ícono de reproducir, como en el mockup
@Composable
fun MiniaturaCapsula(modifier: Modifier = Modifier, ancho: Dp = 52.dp, alto: Dp = 68.dp) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.inverseSurface, modifier = modifier.size(width = ancho, height = alto)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface)
        }
    }
}

//Etiqueta pequeña de color: categoría, estado de la cápsula o estado del punto
@Composable
fun Etiqueta(texto: String, fondo: Color, colorTexto: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(8.dp), color = fondo, contentColor = colorTexto, modifier = modifier) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EtiquetaEstadoCapsula(estado: EstadoCapsula) {
    if (estado == EstadoCapsula.PUBLICADA) {
        Etiqueta("Publicada", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    } else {
        Etiqueta("Borrador", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
fun EtiquetaCategoria(texto: String) {
    Etiqueta(texto, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
}

//58 -> "0:58"
fun textoDuracion(segundos: Int): String = "%d:%02d".format(segundos / 60, segundos % 60)
