package com.cookingplastic.triskeledu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

//Barra de avance entre los dos umbrales (P24 y la tarjeta del mapa); se llena al abrir la pantalla salvo con "Quitar animaciones"
@Composable
fun BarraNivel(
    avance: Float,
    descripcion: String,
    modifier: Modifier = Modifier,
    alto: Dp = 10.dp,
    fondo: Color = MaterialTheme.colorScheme.surface
) {
    val reducirMovimiento = rememberReducirMovimiento()
    val llenado = remember { Animatable(if (reducirMovimiento) avance else 0f) }
    LaunchedEffect(avance) {
        if (reducirMovimiento) llenado.snapTo(avance) else llenado.animateTo(avance, tween(800))
    }
    LinearProgressIndicator(
        progress = { llenado.value },
        strokeCap = StrokeCap.Round,
        color = MaterialTheme.colorScheme.primary,
        trackColor = fondo,
        gapSize = 0.dp,
        drawStopIndicator = {},
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .semantics { contentDescription = descripcion }
    )
}
