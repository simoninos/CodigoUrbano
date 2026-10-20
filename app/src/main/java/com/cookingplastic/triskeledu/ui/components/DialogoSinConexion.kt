package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme

// Texto exacto del ERS para el popup sin conexión (RF-C10)
const val MENSAJE_SIN_CONEXION = "Debes tener conexión a internet para realizar esta acción"

// Pantalla 11 · Sin conexión (D-12, D-13). Dialog oscurece la pantalla de atrás y se cierra
// con el botón, tocando fuera o con el botón atrás.
@Composable
fun DialogoSinConexion(
    lugarDesbloqueado: Boolean,
    alCerrar: () -> Unit
) {
    Dialog(onDismissRequest = alCerrar) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    text = "Sin conexión",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = MENSAJE_SIN_CONEXION,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                if (lugarDesbloqueado) {
                    Text(
                        text = "Tu lugar ya está desbloqueado. El video te espera cuando vuelvas a tener señal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = alCerrar,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Entendido", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Preview
@Composable
private fun DialogoSinConexionPreview() {
    TriskeleduTheme {
        DialogoSinConexion(lugarDesbloqueado = true, alCerrar = {})
    }
}
