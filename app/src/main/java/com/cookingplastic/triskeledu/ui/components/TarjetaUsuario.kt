package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.data.model.NivelUsuario
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.ui.state.avanceNivel
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.validation.ReglaNivel

// Tarjeta del usuario sobre el mapa (RF-C14): inicial, nombre, puntos y lugares escaneados.
// LVL-04: suma "Nv. X", el número sobre el avatar y la barra hasta el siguiente nivel; al tocarla abre P24.
// Sin nivel (administrador, LVL-05) se ve como antes y no responde al toque
@Composable
fun TarjetaUsuario(
    nombre: String,
    puntos: Int,
    lugaresEscaneados: Int,
    totalLugares: Int,
    modifier: Modifier = Modifier,
    nivel: NivelUsuario? = null,
    alPresionar: (() -> Unit)? = null
) {
    val nombreVisible = nombre.ifBlank { "Explorador" }
    val contenido: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarNivel(nombre = nombreVisible, nivel = nivel?.numero)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f, fill = false)) {
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
                        if (nivel != null) {
                            Text(
                                text = "Nv. ${nivel.numero}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                //nunca se corta letra por letra
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Text(
                        text = "$lugaresEscaneados de $totalLugares lugares",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (alPresionar != null && nivel != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (nivel != null) {
                val siguiente = nivel.puntosSiguiente
                BarraNivel(
                    avance = avanceNivel(nivel),
                    descripcion = if (siguiente != null) "Faltan ${siguiente - nivel.puntosTotales} puntos para el nivel ${nivel.numero + 1}"
                    else "Nivel ${nivel.numero}",
                    alto = 6.dp,
                    fondo = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }

    val forma = RoundedCornerShape(20.dp)
    //TalkBack lee "Ver mi nivel" como acción de la tarjeta
    val toque = if (alPresionar != null && nivel != null) {
        Modifier.clickable(onClickLabel = "Ver mi nivel", role = Role.Button, onClick = alPresionar)
    } else {
        Modifier
    }
    Surface(
        modifier = modifier,
        shape = forma,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = toque) { contenido() }
    }
}

@Preview
@Composable
private fun TarjetaUsuarioPreview() {
    TriskeleduTheme(tema = TemaApp.CERRO_SANTA_LUCIA) {
        TarjetaUsuario(
            nombre = "Simoninos", puntos = 60, lugaresEscaneados = 3, totalLugares = 14,
            nivel = ReglaNivel.nivelDesde(60), alPresionar = {}
        )
    }
}

@Preview
@Composable
private fun TarjetaUsuarioNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        TarjetaUsuario(
            nombre = "camila_explora", puntos = 160, lugaresEscaneados = 4, totalLugares = 14,
            nivel = ReglaNivel.nivelDesde(160), alPresionar = {}
        )
    }
}

@Preview
@Composable
private fun TarjetaUsuarioAdminPreview() {
    TriskeleduTheme {
        TarjetaUsuario(nombre = "admin_triskel", puntos = 0, lugaresEscaneados = 0, totalLugares = 14)
    }
}
