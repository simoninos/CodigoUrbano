package com.cookingplastic.triskeledu.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

//PER-03: la campana con el contador de no leídas; abre el historial (P16). Va en Mapa y Mis lugares
@Composable
fun BotonCampana(sinLeer: Int, alPresionar: () -> Unit, modifier: Modifier = Modifier) {
    BadgedBox(
        modifier = modifier,
        badge = { if (sinLeer > 0) Badge { Text(if (sinLeer > 99) "99+" else "$sinLeer") } }
    ) {
        BotonMapa(
            icono = Icons.Outlined.Notifications,
            descripcion = if (sinLeer > 0) "Notificaciones: $sinLeer sin leer" else "Notificaciones",
            alPresionar = alPresionar
        )
    }
}
