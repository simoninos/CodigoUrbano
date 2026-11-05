package com.cookingplastic.triskeledu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.data.nivel.SubidaNivel
import com.cookingplastic.triskeledu.ui.event.SubidaNivelUiEvent
import com.cookingplastic.triskeledu.ui.theme.ColoresApp
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.validation.ReglaNivel

//P25 · ¡Subiste de nivel! (LVL-02). Aparece sobre la pantalla actual; atrás o tocar fuera es "Seguir explorando"
@Composable
fun DialogoSubisteNivel(subida: SubidaNivel, onEvent: (SubidaNivelUiEvent) -> Unit) {
    Dialog(onDismissRequest = { onEvent(SubidaNivelUiEvent.OnSeguirExplorando) }) {
        ContenidoSubisteNivel(subida = subida, onEvent = onEvent)
    }
}

@Composable
private fun ContenidoSubisteNivel(subida: SubidaNivel, onEvent: (SubidaNivelUiEvent) -> Unit) {
    val nivel = subida.nivel
    val reducirMovimiento = rememberReducirMovimiento()

    //la estrella entra girando y el avatar "salta"; con "Quitar animaciones" quedan quietos desde el inicio
    val escalaEstrella = remember { Animatable(if (reducirMovimiento) 1f else 0f) }
    val giroEstrella = remember { Animatable(if (reducirMovimiento) 0f else -120f) }
    val escalaAvatar = remember { Animatable(if (reducirMovimiento) 1f else 0.6f) }
    LaunchedEffect(Unit) {
        if (reducirMovimiento) return@LaunchedEffect
        escalaAvatar.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    LaunchedEffect(Unit) {
        if (reducirMovimiento) return@LaunchedEffect
        giroEstrella.animateTo(0f, tween(600))
    }
    LaunchedEffect(Unit) {
        if (reducirMovimiento) return@LaunchedEffect
        escalaEstrella.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = ColoresApp.puntos,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = escalaEstrella.value
                        scaleY = escalaEstrella.value
                        rotationZ = giroEstrella.value
                    }
            )
            AvatarNivel(
                nombre = subida.nombreUsuario,
                nivel = nivel.numero,
                tamano = 72.dp,
                estiloInicial = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.graphicsLayer {
                    scaleX = escalaAvatar.value
                    scaleY = escalaAvatar.value
                }
            )
            Text(
                text = "¡Subiste de nivel!",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Nivel ${nivel.numero} · ${nivel.nombre}",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = if (subida.tema != null) "Llegaste a ${nivel.puntosTotales} puntos. Desbloqueaste un tema nuevo:"
                else "Llegaste a ${nivel.puntosTotales} puntos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (subida.tema != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MuestraColores(tema = subida.tema, tamano = 18.dp)
                        Text(text = subida.tema.nombre, style = MaterialTheme.typography.titleSmall)
                    }
                }
                Button(
                    onClick = { onEvent(SubidaNivelUiEvent.OnUsarTema) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("Usar este tema") }
            }
            OutlinedButton(
                onClick = { onEvent(SubidaNivelUiEvent.OnSeguirExplorando) },
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text("Seguir explorando") }
        }
    }
}

private val subidaPreview = SubidaNivel(
    nivel = ReglaNivel.nivelDesde(110),
    tema = TemaApp.ATARDECER_MAPOCHO,
    nombreUsuario = "Simoninos"
)

@Preview(name = "Subiste de nivel", showBackground = true)
@Composable
private fun SubisteNivelPreview() {
    TriskeleduTheme {
        ContenidoSubisteNivel(subida = subidaPreview, onEvent = {})
    }
}

@Preview(name = "Subiste de nivel (noche, IA neón)", showBackground = true)
@Composable
private fun SubisteNivelNochePreview() {
    TriskeleduTheme(darkTheme = true, tema = TemaApp.IA_NEON) {
        ContenidoSubisteNivel(subida = subidaPreview.copy(nivel = ReglaNivel.nivelDesde(990), tema = TemaApp.IA_NEON), onEvent = {})
    }
}
