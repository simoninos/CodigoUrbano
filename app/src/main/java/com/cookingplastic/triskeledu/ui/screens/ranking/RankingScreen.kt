package com.cookingplastic.triskeledu.ui.screens.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.ui.components.rememberReducirMovimiento
import com.cookingplastic.triskeledu.ui.event.RankingUiEvent
import com.cookingplastic.triskeledu.ui.state.FilaRanking
import com.cookingplastic.triskeledu.ui.state.PeriodoRanking
import com.cookingplastic.triskeledu.ui.state.RankingUiState
import com.cookingplastic.triskeledu.ui.theme.ColoresApp
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.RankingViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.armarRanking

//P20 · Ranking (LUG-06): desde "Ver ranking ›" en la tarjeta "Mis puntos" de P07-A
@Composable
fun RankingScreen(
    alVolver: () -> Unit,
    viewModel: RankingViewModel = viewModel(factory = RankingViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    RankingContent(estado = estado, onEvent = viewModel::onEvent, alVolver = alVolver)
}

@Composable
fun RankingContent(
    estado: RankingUiState,
    onEvent: (RankingUiEvent) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    //RNF-09: las filas se reordenan con animación al cambiar de periodo, salvo que se pida reducir el movimiento
    val reducirMovimiento = rememberReducirMovimiento()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                Text("Ranking", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodoRanking.entries.forEach { periodo ->
                    FilterChip(
                        selected = estado.periodo == periodo,
                        onClick = { onEvent(RankingUiEvent.OnPeriodoChange(periodo)) },
                        label = { Text(periodo.etiqueta) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }
        if (!estado.cargando && estado.primeros.isEmpty()) {
            item {
                Text(
                    "Todavía no hay jugadores en este teléfono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }
        items(estado.primeros, key = { it.usuarioId }) { fila ->
            FilaRankingItem(fila, modifier = if (reducirMovimiento) Modifier else Modifier.animateItem())
        }
        estado.filaActual?.let { fila ->
            //el usuario conectado está más abajo: se muestra aparte, después de "···"
            item(key = "separador") {
                Text(
                    "···",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }
                )
            }
            item(key = "actual") {
                FilaRankingItem(fila, modifier = if (reducirMovimiento) Modifier else Modifier.animateItem())
            }
        }
        item {
            Text(
                "Suma puntos escaneando QR (+10), viendo cápsulas (+50) y subiendo selfies (+20).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

//Posición, inicial, nombre y puntos. El usuario conectado va resaltado y con "Tú"
@Composable
private fun FilaRankingItem(fila: FilaRanking, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (fila.esActual) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (fila.esActual) 2.dp else 1.dp, if (fila.esActual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "Puesto ${fila.posicion}, ${fila.username}${if (fila.esActual) ", tú" else ""}, ${fila.puntos} puntos"
        }
    ) {
        Row(modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            //1, 2 y 3 con color de medalla; el resto neutro
            val (fondoPosicion, textoPosicion) = when (fila.posicion) {
                1 -> ColoresApp.puntos to ColoresApp.sobrePuntos
                2 -> MaterialTheme.colorScheme.outlineVariant to MaterialTheme.colorScheme.onSurface
                3 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(modifier = Modifier.size(32.dp).background(fondoPosicion, CircleShape), contentAlignment = Alignment.Center) {
                Text("${fila.posicion}", style = MaterialTheme.typography.labelLarge, color = textoPosicion)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(36.dp).background(
                    if (fila.esActual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    fila.username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (fila.esActual) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fila.username, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (fila.esActual) {
                    Text("Tú", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "${fila.puntos} pts",
                style = MaterialTheme.typography.labelLarge,
                color = ColoresApp.sobrePuntos,
                modifier = Modifier.background(ColoresApp.puntos, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Preview(name = "Ranking", showBackground = true)
@Composable
private fun RankingPreview() {
    val (primeros, actual) = armarRanking(
        DatosPrueba.usuarios, DatosPrueba.progreso, DatosPrueba.USUARIO_EXPLORADOR.id, PeriodoRanking.SIEMPRE, System.currentTimeMillis()
    )
    TriskeleduTheme {
        Surface {
            RankingContent(
                estado = RankingUiState(periodo = PeriodoRanking.SIEMPRE, primeros = primeros, filaActual = actual, cargando = false),
                onEvent = {}, alVolver = {}
            )
        }
    }
}
