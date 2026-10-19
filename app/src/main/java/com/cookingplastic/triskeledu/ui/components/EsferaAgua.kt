package com.cookingplastic.triskeledu.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import kotlin.math.PI
import kotlin.math.sin

//Esfera de avance de un lugar: el agua sube según los puntos y la superficie es una ola que se mueve.
//La usan Mapa y Detalle (Simon) y Mis lugares. Solo recibe números, no lee ningún ViewModel.
@Composable
fun EsferaAgua(
    puntos: Int,
    maximo: Int,
    modifier: Modifier = Modifier
) {
    val nivel: Float =
        if (maximo <= 0) 0f
        //Si el maximo llega a 0 o menos, el nivel se deja como 0
        else (puntos.toFloat() / maximo.toFloat()).coerceIn(0f, 1f)

    //cuando cambia el nivel, el agua no salta: sube de a poco en casi un segundo
    val nivelAnimado by animateFloatAsState(
        targetValue = nivel,
        animationSpec = tween(durationMillis = 900),
        label = "nivelAgua"
    )

    //un número que va de 0 a una vuelta completa (2π) y vuelve a empezar sin parar: mueve la ola
    val transicionOla = rememberInfiniteTransition(label = "ola")
    val fase by transicionOla.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "faseOla"
    )

    //colores solo desde el tema, como pide el plan (tertiary es Agua)
    val colorAgua = MaterialTheme.colorScheme.tertiary
    val colorBorde = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, colorBorde, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        //Canvas es una hoja para dibujar a mano; matchParentSize la hace del mismo tamaño que la esfera
        Canvas(modifier = Modifier.matchParentSize()) {
            if (nivelAnimado > 0f) {
                //altura (en píxeles) donde está la superficie del agua, contando desde arriba
                val superficie = size.height * (1f - nivelAnimado)
                //la ola se aplana al llenarse, para que 20/20 se vea completa
                val altoOla = size.height * 0.06f * (1f - nivelAnimado)

                //Path es un contorno: se baja a la esquina, se sube por la izquierda,
                //se recorre la ola de izquierda a derecha y se cierra por abajo
                val agua = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, superficie)
                    val pasos = 24
                    for (i in 0..pasos) {
                        val x = size.width * i / pasos
                        val y = superficie + altoOla * sin(fase + (x / size.width) * 2f * PI.toFloat())
                        lineTo(x, y)
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(path = agua, color = colorAgua)
            }
        }

        Text(
            text = "$puntos/$maximo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

//Las tres esferas del sistema visual: vacía, llenándose y llena
@Preview(showBackground = true)
@Composable
fun EsferaAguaPreview() {
    TriskeleduTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            EsferaAgua(puntos = 0, maximo = 20)
            EsferaAgua(puntos = 5, maximo = 20)
            EsferaAgua(puntos = 20, maximo = 20)
        }
    }
}