package com.cookingplastic.triskeledu.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

//true si la persona activó "Quitar animaciones" en la accesibilidad del teléfono.
//Las transiciones de la app lo respetan: en ese caso el cambio es inmediato
@Composable
fun rememberReducirMovimiento(): Boolean {
    val contexto = LocalContext.current
    return remember(contexto) {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
