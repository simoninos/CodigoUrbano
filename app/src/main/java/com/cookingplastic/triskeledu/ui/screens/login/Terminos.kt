package com.cookingplastic.triskeledu.ui.screens.login

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme

//Términos y condiciones (versión 1.1, RegistroViewModel.VERSION_TERMINOS). La 1.1 agrega la ubicación en segundo
//plano para "Lugar cercano" (RNF-03)
const val TEXTO_TERMINOS = """Aprendo IA Jugando en Santiago es un juego educativo: escaneas el QR de lugares del centro de Santiago, ves una cápsula de video sobre inteligencia artificial y sumas puntos.

Tu cuenta. Guardamos tu nombre de usuario, tu correo y tu contraseña (protegida con hash, nunca como texto). Tus puntos, los lugares que escaneas, tus comentarios y tus selfies quedan asociados a tu cuenta. Los comentarios los ven todos los usuarios.

Ubicación. La usamos para mostrarte en el mapa, medir la distancia a cada lugar y validar el escaneo (100 m o menos).

Ubicación en segundo plano. Si lo permites ("Permitir todo el tiempo"), también la usamos con la app minimizada, solo para avisarte cuando estés a 200 m de un lugar por descubrir. Mientras eso ocurre verás una notificación fija. Puedes apagarlo cuando quieras en Configuración ("Lugares cercanos" o "Seguir mi ubicación") o quitando el permiso en los ajustes del teléfono. Si no lo permites, el aviso funciona solo con la app abierta.

Cámara. Se usa para leer los códigos QR y tomar las selfies de cada lugar.

Notificaciones. Te avisamos de lugares cercanos, cápsulas nuevas y logros. Puedes apagar cada tipo en Configuración."""

//Se abre con "Leer los términos" en el registro (P02)
@Composable
fun DialogoTerminos(alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Términos y condiciones") },
        text = {
            Text(
                TEXTO_TERMINOS,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = alCerrar, modifier = Modifier.heightIn(min = 48.dp)) { Text("Entendido") }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun DialogoTerminosPreview() {
    TriskeleduTheme { DialogoTerminos(alCerrar = {}) }
}
