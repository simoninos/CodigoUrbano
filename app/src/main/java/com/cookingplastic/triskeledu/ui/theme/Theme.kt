package com.cookingplastic.triskeledu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.cookingplastic.triskeledu.data.model.TemaApp

@Composable
fun TriskeleduTheme(
    // Día por defecto (se lee mejor al sol). Configuración entrega aquí la preferencia temaNoche.
    darkTheme: Boolean = false,
    // LVL-03: el tema elegido en Apariencia; las previews usan el Clásico
    tema: TemaApp = TemaApp.CLASICO,
    content: @Composable () -> Unit
) {
    val paleta = paletaDe(tema, darkTheme)
    CompositionLocalProvider(LocalPaletaTema provides paleta) {
        MaterialTheme(
            colorScheme = paleta.esquema,
            typography = Typography,
            content = content
        )
    }
}

// Colores marcados "(propio)" en el plan: no existen en Material 3, así que se leen de la paleta
// del tema activo (Día o Noche de cualquiera de los 8 temas)
object ColoresApp {
    private val paleta: PaletaTema
        @Composable @ReadOnlyComposable get() = LocalPaletaTema.current

    val esNoche: Boolean
        @Composable @ReadOnlyComposable get() = paleta.esNoche

    val ubicacion: Color
        @Composable @ReadOnlyComposable get() = paleta.ubicacion

    val bloqueado: Color
        @Composable @ReadOnlyComposable get() = paleta.bloqueado

    val agua: Color
        @Composable @ReadOnlyComposable get() = paleta.agua

    val puntos: Color
        @Composable @ReadOnlyComposable get() = paleta.puntos

    val sobrePuntos: Color
        @Composable @ReadOnlyComposable get() = paleta.sobrePuntos

    val barraAdmin: Color
        @Composable @ReadOnlyComposable get() = paleta.barraAdmin

    val sobreBarraAdmin: Color
        @Composable @ReadOnlyComposable get() = paleta.sobreBarraAdmin

    val interruptorEncendido: Color
        @Composable @ReadOnlyComposable get() = paleta.interruptorEncendido
}
