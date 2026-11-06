package com.cookingplastic.triskeledu.ui.screens.bienvenida

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.SesionViewModel

//P00 (ACC-01): la primera pantalla sin sesión. No tiene UiState propio; usa SesionViewModel
//solo para saber si ya había una sesión guardada y, si la hay, ir directo al mapa
@Composable
fun BienvenidaScreen(
    alCrearCuenta: () -> Unit,
    alTengoCuenta: () -> Unit,
    alSesionIniciada: () -> Unit,
    viewModel: SesionViewModel = viewModel(factory = SesionViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.sesionIniciada) {
        if (estado.sesionIniciada) alSesionIniciada()
    }

    //mientras Room responde (o si hay sesión) no se dibuja la bienvenida, para que no aparezca un instante
    if (estado.sesionComprobada && !estado.sesionIniciada) {
        BienvenidaContent(alCrearCuenta = alCrearCuenta, alTengoCuenta = alTengoCuenta)
    } else {
        Box(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun BienvenidaContent(
    alCrearCuenta: () -> Unit,
    alTengoCuenta: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //lugar del logo de Triskeledu, como en el mockup
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = "Logo de Triskeledu",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aprendo IA Jugando en Santiago",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Recorre la ciudad, descubre lugares y aprende sobre inteligencia artificial.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        //los tres pasos del juego
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Paso(1, Icons.Outlined.Map, "Explora el mapa", "Descubre puntos de interés de Santiago Centro.")
            Paso(2, Icons.Outlined.QrCodeScanner, "Escanea el QR", "A 100 m o menos del lugar, desbloquéalo con la cámara.")
            Paso(3, Icons.Outlined.Videocam, "Aprende y suma", "Mira la cápsula sobre IA y gana puntos.")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = alCrearCuenta, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Crear cuenta")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = alTengoCuenta, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Ya tengo cuenta")
        }
    }
}

@Composable
private fun Paso(numero: Int, icono: ImageVector, titulo: String, texto: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column {
                Text("PASO $numero", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BienvenidaPreview() {
    TriskeleduTheme { Surface { BienvenidaContent(alCrearCuenta = {}, alTengoCuenta = {}) } }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BienvenidaNochePreview() {
    TriskeleduTheme(darkTheme = true) { Surface { BienvenidaContent(alCrearCuenta = {}, alTengoCuenta = {}) } }
}
