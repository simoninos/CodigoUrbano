package com.cookingplastic.triskeledu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimarioNoche,
    onPrimary = SobrePrimarioNoche,
    primaryContainer = PrimarioContenedorNoche,
    onPrimaryContainer = SobreContenedorNoche,
    secondary = PuntosNoche,
    onSecondary = SobrePuntosNoche,
    secondaryContainer = PuntosContenedorNoche,
    onSecondaryContainer = SobrePuntosContenedorNoche,
    tertiary = AguaNoche,
    onTertiary = TintaNoche,
    background = FondoNoche,
    onBackground = TintaNoche,
    surface = FondoNoche,
    onSurface = TintaNoche,
    surfaceVariant = SuperficieNoche,
    onSurfaceVariant = TintaSecundariaNoche,
    outline = BordeNoche,
    error = AlertaNoche,
    onError = SobreAlertaNoche,
)

private val LightColorScheme = lightColorScheme(
    primary = Primario,
    onPrimary = Fondo,
    primaryContainer = PrimarioClaro,
    onPrimaryContainer = PrimarioOscuro,
    secondary = Puntos,
    onSecondary = Tinta,
    secondaryContainer = PuntosClaro,
    onSecondaryContainer = Tinta,
    tertiary = Agua,
    onTertiary = Tinta,
    background = Fondo,
    onBackground = Tinta,
    surface = Fondo,
    onSurface = Tinta,
    surfaceVariant = Superficie,
    onSurfaceVariant = TintaSecundaria,
    outline = Borde,
    error = Alerta,
    onError = Fondo,
)

@Composable
fun TriskeleduTheme(
    // Día por defecto (se lee mejor al sol). Configuración entrega aquí la preferencia temaNoche.
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}