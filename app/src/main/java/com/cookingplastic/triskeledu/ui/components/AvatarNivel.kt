package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

//LVL-04: círculo con la inicial y, encima, el número del nivel en un círculo de color Puntos.
//Sin nivel (administrador) se ve solo la inicial
@Composable
fun AvatarNivel(
    nombre: String,
    nivel: Int?,
    modifier: Modifier = Modifier,
    tamano: Dp = 44.dp,
    estiloInicial: TextStyle = MaterialTheme.typography.titleLarge
) {
    val inicial = nombre.ifBlank { "?" }.first().uppercaseChar().toString()
    //el círculo del nivel sobresale un poco hacia abajo y a la derecha, como en el mockup
    Box(modifier = modifier.size(tamano + 6.dp)) {
        Box(
            modifier = Modifier
                .size(tamano)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = inicial, style = estiloInicial, color = MaterialTheme.colorScheme.onPrimary)
        }
        if (nivel != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(tamano * 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "$nivel", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
