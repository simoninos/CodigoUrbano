package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun Boton(onClicked: () -> Unit, icon: ImageVector = Icons.Default.Home){
    IconButton(
        onClick = onClicked,
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.secondary, shape = CircleShape)
    ){
        Icon(
            modifier = Modifier
                .padding(10.dp),
            imageVector = icon,
            tint = MaterialTheme.colorScheme.inversePrimary,
            contentDescription = "Opciones",
        )
    }
}


// Botón cuadrado que flota sobre el mapa (engranaje y "Centrar"): 56 dp, esquinas de 18 dp,
// fondo de superficie y borde gris, como en el mockup
@Composable
fun BotonMapa(
    icono: ImageVector,
    descripcion: String,
    alPresionar: () -> Unit,
    modifier: Modifier = Modifier,
    colorIcono: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = alPresionar,
        modifier = modifier.size(56.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icono,
                contentDescription = descripcion,
                tint = colorIcono,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun SwitchModoOscuro(checked: Boolean, onChecked: (Boolean)-> Unit) {
    Switch(
        checked = checked, onCheckedChange = onChecked,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Filled.LightMode,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        }
    )
}

@Preview
@Composable
fun SwitchModoOscuroPreview(){
    var isChecked by remember { mutableStateOf(false) }

    SwitchModoOscuro(false, onChecked = {isChecked = it})
}

@Preview
@Composable
fun BotonesPreview(){
    Column(modifier = Modifier
        .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ){
        // Botón de opción
        Boton(onClicked = {}, icon = Icons.Default.Settings)

        // Botón de Return
        Boton(onClicked = {}, icon = Icons.Default.TurnLeft)

        // Botón del mapa
        BotonMapa(icono = Icons.Default.Settings, descripcion = "Abrir configuración", alPresionar = {})
    }
}
