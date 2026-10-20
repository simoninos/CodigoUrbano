package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme

// Tarjeta del usuario sobre el mapa (RF-C14): inicial, nombre, puntos y lugares escaneados
@Composable
fun TarjetaUsuario(
    nombre: String,
    puntos: Int,
    lugaresEscaneados: Int,
    totalLugares: Int,
    modifier: Modifier = Modifier
) {
    val nombreVisible = nombre.ifBlank { "Explorador" }
    val inicial = nombreVisible.first().uppercaseChar().toString()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Círculo con la inicial del nombre
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = inicial,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = nombreVisible,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Etiqueta amarilla con los puntos
                    Text(
                        text = "$puntos pts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 1.dp)
                    )
                    Text(
                        text = "$lugaresEscaneados de $totalLugares lugares",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun TarjetaUsuarioPreview() {
    TriskeleduTheme {
        TarjetaUsuario(nombre = "cliente_prueba", puntos = 45, lugaresEscaneados = 3, totalLugares = 13)
    }
}

@Preview
@Composable
private fun TarjetaUsuarioNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        TarjetaUsuario(nombre = "cliente_prueba", puntos = 45, lugaresEscaneados = 3, totalLugares = 13)
    }
}
