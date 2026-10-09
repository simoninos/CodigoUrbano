package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun BotonVolverPreview(){
    BotonVolver("Hola")
}

@Preview
@Composable
fun BotonOpcionesPreview(){
    BotonOpciones("Hola")
}

@Preview
@Composable
fun SwitchModoOscuroPreview(){
    SwitchModoOscuro()
}

@Composable
fun BotonVolver(route: String){
    IconButton( onClick = { route }
    ){
        Icon(
            imageVector = Icons.Default.TurnLeft,
            contentDescription = "Volver"
        )
    }
}

@Composable
fun BotonOpciones(route: String){
    IconButton( onClick = { route }
    ){
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Opciones"
        )
    }
}

@Composable
fun SwitchModoOscuro() {
    var checked by remember { mutableStateOf(true) }

    Switch(
        checked = checked,
        onCheckedChange = {
            checked = it
        },
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