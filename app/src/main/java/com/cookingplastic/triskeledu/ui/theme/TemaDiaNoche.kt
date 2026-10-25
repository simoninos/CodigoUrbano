package com.cookingplastic.triskeledu.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.ui.viewmodels.TemaViewModel

//Envuelve toda la app (va en MainActivity): elige los colores de día o de noche según la preferencia temaNoche
@Composable
fun TemaDiaNoche(
    viewModel: TemaViewModel = viewModel(factory = TemaViewModel.Factory),
    content: @Composable () -> Unit
) {
    val temaNoche by viewModel.temaNoche.collectAsStateWithLifecycle()

    //los íconos de la barra de estado (hora, batería) siguen al tema de la app y no al del teléfono;
    //si no, en modo noche quedarían oscuros sobre fondo oscuro
    val actividad = LocalActivity.current as? ComponentActivity
    LaunchedEffect(temaNoche) {
        val estilo = if (temaNoche) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        actividad?.enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
    }

    TriskeleduTheme(darkTheme = temaNoche, content = content)
}
