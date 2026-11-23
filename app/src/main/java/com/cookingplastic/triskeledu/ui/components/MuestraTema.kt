package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.ui.theme.paletaMuestra

//Tres cuadritos con los colores de un tema (primario, fondo y puntos), como en P24 y P25.
//Los colores salen de la paleta del tema en ui/theme, no se escriben aquí
@Composable
fun MuestraColores(tema: TemaApp, modifier: Modifier = Modifier, tamano: Dp = 14.dp) {
    val paleta = paletaMuestra(tema)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(paleta.primario, paleta.fondo, paleta.puntos).forEach { color ->
            Box(
                modifier = Modifier
                    .size(tamano)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            )
        }
    }
}

//Círculo partido en diagonal (fondo arriba, primario abajo) para la fila de temas de Apariencia
@Composable
fun CirculoTema(tema: TemaApp, modifier: Modifier = Modifier, tamano: Dp = 48.dp) {
    val paleta = paletaMuestra(tema)
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .drawBehind {
                drawRect(paleta.fondo)
                val diagonal = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(diagonal, paleta.primario)
            }
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}
