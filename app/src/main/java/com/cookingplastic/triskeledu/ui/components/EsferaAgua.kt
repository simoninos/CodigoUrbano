package com.cookingplastic.triskeledu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EsferaAgua(
    puntos: Int,
    maximo: Int,
    modifier: Modifier = Modifier
) {
    val nivel: Float =
        if (maximo<=0)0f
        //Si el maximo llega a 0 o menos, el nivel se deja como 0
    else(puntos.toFloat()/ maximo.toFloat()).coerceIn(0f,1f)
    // 

}