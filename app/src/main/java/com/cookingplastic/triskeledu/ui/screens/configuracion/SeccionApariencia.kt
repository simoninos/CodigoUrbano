package com.cookingplastic.triskeledu.ui.screens.configuracion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.ui.components.CirculoTema

//LVL-03: fila de los 8 temas con su muestra de color. Los bloqueados se ven atenuados con "Nv. X",
//no responden al toque y TalkBack dice en qué nivel se desbloquean
@Composable
fun SeccionApariencia(
    tema: TemaApp,
    temasDesbloqueados: Set<TemaApp>,
    alElegir: (TemaApp) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Tema", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = tema.nombre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            //se desplaza de lado si no caben los 8
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TemaApp.entries.forEach { opcion ->
                    OpcionTema(
                        tema = opcion,
                        elegido = opcion == tema,
                        bloqueado = opcion !in temasDesbloqueados,
                        alElegir = { alElegir(opcion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OpcionTema(tema: TemaApp, elegido: Boolean, bloqueado: Boolean, alElegir: () -> Unit) {
    val toque = if (bloqueado) {
        Modifier.clearAndSetSemantics {
            contentDescription = "Tema ${tema.nombre}, bloqueado. Se desbloquea en el nivel ${tema.nivelRequerido}"
            disabled()
        }
    } else {
        Modifier
            .selectable(selected = elegido, role = Role.RadioButton, onClick = alElegir)
            .semantics { contentDescription = "Tema ${tema.nombre}" }
    }
    Column(
        modifier = Modifier
            .width(68.dp)
            .then(toque)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CirculoTema(
                tema = tema,
                tamano = 52.dp,
                modifier = Modifier
                    .alpha(if (bloqueado) 0.4f else 1f)
                    .then(
                        if (elegido) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), CircleShape)
                        else Modifier
                    )
            )
            if (bloqueado) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(26.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (elegido) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        Text(
            text = nombreCorto(tema),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = if (bloqueado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        if (bloqueado) {
            Text(
                text = "Nv. ${tema.nivelRequerido}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

//nombres cortos para que quepan bajo el círculo, como en el mockup
private fun nombreCorto(tema: TemaApp): String = when (tema) {
    TemaApp.CLASICO -> "Clásico"
    TemaApp.CERRO_SANTA_LUCIA -> "Santa Lucía"
    TemaApp.ATARDECER_MAPOCHO -> "Mapocho"
    TemaApp.NOCHE_MUSEO -> "Museo"
    TemaApp.DESIERTO_FLORIDO -> "Desierto"
    TemaApp.BOSQUE_NATIVO -> "Bosque"
    TemaApp.GLACIAR -> "Glaciar"
    TemaApp.IA_NEON -> "Neón"
}
