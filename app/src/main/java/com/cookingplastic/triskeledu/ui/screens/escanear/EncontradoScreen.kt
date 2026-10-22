package com.cookingplastic.triskeledu.ui.screens.escanear

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.data.model.PUNTOS_ESCANEO
import com.cookingplastic.triskeledu.data.model.PUNTOS_VIDEO
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme

//Pantalla 6: aparece cuando el escaneo es válido. Solo dibuja: recibe los datos y avisa los clics.
//Registrar el escaneo (fecha, ubicación, +5) lo hace el ViewModel con ProgresoRepository
@Composable
fun EncontradoContent(
    nombreLugar: String,
    tipoLugar: String,
    distanciaMetros: Int?,
    yaDescubierto: Boolean,
    alAbrirLugar: () -> Unit,
    alSeguirEscaneando: () -> Unit,
    modifier: Modifier = Modifier
) {
    //empieza invisible y al dibujarse pide ser visible: eso dispara la celebración una sola vez
    val celebracion = remember { MutableTransitionState(false).apply { targetState = true } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
    ) {
        //parte de arriba: el check amarillo, que aparece creciendo
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            //va con su nombre completo porque este Box está dentro de una Column:
            //así Kotlin no lo confunde con la versión de AnimatedVisibility que es solo para Column
            androidx.compose.animation.AnimatedVisibility(
                visibleState = celebracion,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }

        //parte de abajo: la tarjeta con los datos del lugar
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "¡LUGAR ENCONTRADO!",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = nombreLugar,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Etiqueta(texto = tipoLugar)
                    if (distanciaMetros != null) {
                        Text(
                            text = "a $distanciaMetros m de ti",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (yaDescubierto) {
                    //si ya lo había escaneado, no hay celebración: los puntos no cambian
                    Text(
                        text = "Ya habías descubierto este lugar",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    //la celebración "+5 puntos": crece desde chica y aparece de a poco
                    AnimatedVisibility(
                        visibleState = celebracion,
                        enter = scaleIn(
                            initialScale = 0.3f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        ) + fadeIn()
                    ) {
                        Text(
                            text = "+$PUNTOS_ESCANEO puntos",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = "Desbloqueaste el video sobre IA de este lugar. Míralo completo para ganar $PUNTOS_VIDEO puntos más.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                //el escaneo queda con sincronizado = false hasta que exista el backend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Se sincroniza cuando tengas conexión",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                Button(
                    onClick = alAbrirLugar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Abrir lugar")
                }
                TextButton(onClick = alSeguirEscaneando) {
                    Text("Seguir escaneando")
                }
            }
        }
    }
}

//Etiqueta gris con el tipo del lugar, como "Monumento" en el mockup
@Composable
private fun Etiqueta(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun EncontradoPreview() {
    TriskeleduTheme {
        EncontradoContent(
            nombreLugar = "La Moneda",
            tipoLugar = "Monumento",
            distanciaMetros = 34,
            yaDescubierto = false,
            alAbrirLugar = {},
            alSeguirEscaneando = {}
        )
    }
}