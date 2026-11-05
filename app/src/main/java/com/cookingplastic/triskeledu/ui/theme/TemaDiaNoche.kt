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

//Envuelve toda la app (va en MainActivity): elige la paleta por el tema elegido (LVL-03) y la preferencia temaNoche.
//Al cambiar cualquiera de los dos, toda la app se vuelve a dibujar con los colores nuevos sin perder lo escrito (EXC-14)
@Composable
fun TemaDiaNoche(
    viewModel: TemaViewModel = viewModel(factory = TemaViewModel.Factory),
    content: @Composable () -> Unit
) {
    val temaNoche by viewModel.temaNoche.collectAsStateWithLifecycle()
    val tema by viewModel.tema.collectAsStateWithLifecycle()
    //la barra de estado depende del fondo de la paleta, no solo del interruptor
    val fondoOscuro = paletaDe(tema, temaNoche).esNoche

    //los íconos de la barra de estado (hora, batería) siguen al tema de la app y no al del teléfono;
    //si no, en modo noche quedarían oscuros sobre fondo oscuro
    val actividad = LocalActivity.current as? ComponentActivity
    LaunchedEffect(fondoOscuro) {
        val estilo = if (fondoOscuro) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        actividad?.enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
    }

    TriskeleduTheme(darkTheme = temaNoche, tema = tema, content = content)
}
