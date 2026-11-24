package com.cookingplastic.triskeledu.ui.screens.nivel

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.ui.components.AvatarNivel
import com.cookingplastic.triskeledu.ui.components.BarraNivel
import com.cookingplastic.triskeledu.ui.components.MuestraColores
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme

//P24 · Mi nivel (LVL-01): se abre al tocar la tarjeta del usuario en el mapa
@Composable
fun NivelScreen(
    alVolver: () -> Unit,
    alCambiarTema: () -> Unit,
    viewModel: NivelViewModel = viewModel(factory = NivelViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    NivelContent(estado = estado, alVolver = alVolver, alCambiarTema = alCambiarTema)
}

@Composable
fun NivelContent(
    estado: NivelUiState,
    alVolver: () -> Unit,
    alCambiarTema: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                    Text(
                        text = "Mi nivel",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                }
            }
            item { TarjetaNivel(estado) }
            item {
                Text(
                    text = "Sube de nivel sumando puntos: QR +10, cápsula +50, selfie +20. Cada nivel desbloquea un tema para la app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Text(
                    text = "Niveles y temas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )
            }
            items(estado.filas, key = { it.numero }) { fila -> FilaDeNivel(fila) }
        }
        //"Cambiar tema" abre Configuración, donde está Apariencia
        Button(
            onClick = alCambiarTema,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 52.dp)
        ) {
            Icon(Icons.Outlined.Layers, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cambiar tema")
        }
    }
}

//Cabecera: avatar, número y nombre del nivel, puntos y la barra hasta el siguiente
@Composable
private fun TarjetaNivel(estado: NivelUiState) {
    val nivel = estado.nivel
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AvatarNivel(
                    nombre = estado.nombreUsuario,
                    nivel = nivel.numero,
                    tamano = 64.dp,
                    estiloInicial = MaterialTheme.typography.headlineSmall
                )
                Column {
                    Text(text = "Nivel ${nivel.numero}", style = MaterialTheme.typography.labelMedium)
                    Text(text = nivel.nombre, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "${nivel.puntosTotales} puntos en total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val faltan = estado.faltanParaSiguiente
            val textoBarra = if (faltan != null) "Te faltan $faltan pts para el nivel ${nivel.numero + 1}" else "Nivel máximo"
            BarraNivel(
                avance = estado.avance,
                descripcion = textoBarra
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${nivel.puntosDesde}", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = textoBarra,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text(text = "${nivel.puntosSiguiente ?: ""}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

//Una fila: número en un círculo, nombre y umbral, tema con su muestra y el estado a la derecha
@Composable
private fun FilaDeNivel(fila: FilaNivel) {
    val actual = fila.estado == EstadoFilaNivel.NIVEL_ACTUAL
    val bloqueado = fila.estado == EstadoFilaNivel.BLOQUEADO
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (actual) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (actual) 2.dp else 1.dp, if (actual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (bloqueado) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${fila.numero}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (bloqueado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                //nombre y umbral en un solo texto: si no cabe, el umbral baja de línea completo
                val colorUmbral = MaterialTheme.colorScheme.onSurfaceVariant
                val estiloUmbral = MaterialTheme.typography.bodySmall
                Text(
                    text = buildAnnotatedString {
                        append(fila.nombre)
                        append("  ")
                        withStyle(estiloUmbral.toSpanStyle().copy(color = colorUmbral)) { append("${fila.umbral}\u00A0pts") }
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MuestraColores(tema = fila.tema)
                    Text(
                        text = if (fila.tema == TemaApp.CLASICO) "Tema Día y Noche" else "Tema ${fila.tema.nombre}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            EstadoDeFila(fila)
        }
    }
}

@Composable
private fun EstadoDeFila(fila: FilaNivel) {
    //ancho máximo para que el nombre del nivel conserve su espacio
    Box(modifier = Modifier.widthIn(max = 112.dp), contentAlignment = Alignment.CenterEnd) { TextoEstado(fila) }
}

@Composable
private fun TextoEstado(fila: FilaNivel) {
    when (fila.estado) {
        EstadoFilaNivel.NIVEL_ACTUAL -> Text(
            text = "Nivel actual",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        EstadoFilaNivel.DESBLOQUEADO -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(text = "Desbloqueado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        EstadoFilaNivel.BLOQUEADO -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Text(
                text = "Faltan ${fila.faltanPuntos} pts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Mi nivel", showBackground = true)
@Composable
private fun NivelPreview() {
    TriskeleduTheme(tema = TemaApp.CERRO_SANTA_LUCIA) {
        Surface {
            NivelContent(estado = armarNivel(puntos = 60, nivelMaximoAlcanzado = 2).copy(nombreUsuario = "Simoninos"), alVolver = {}, alCambiarTema = {})
        }
    }
}

@Preview(name = "Mi nivel (noche)", showBackground = true)
@Composable
private fun NivelNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        Surface {
            NivelContent(estado = armarNivel(puntos = 160, nivelMaximoAlcanzado = 3).copy(nombreUsuario = "camila_explora"), alVolver = {}, alCambiarTema = {})
        }
    }
}
