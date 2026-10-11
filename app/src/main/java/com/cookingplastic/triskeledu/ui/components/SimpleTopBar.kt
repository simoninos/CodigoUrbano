package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.navigation.Destination
import com.cookingplastic.triskeledu.ui.screens.mapa.MapOverlay

@Preview
@Composable
fun SimpleTopBarPreview(){
    SimpleTopBar(Destination.MAPA)
}

@Composable
fun SimpleTopBar (destination: Destination){
    val colorFondo = when (destination) {
        Destination.MAPA -> MaterialTheme.colorScheme.primary
        Destination.QR -> Color.Transparent
        Destination.MONUMENTOS -> Color.Transparent
        Destination.CONFIGURACION -> Color.Transparent
    }

    Surface(color = colorFondo){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            when (destination) {
                Destination.MAPA -> {
                    MapOverlay(true)
                    BotonOpciones("Screen Opciones")
                }
                Destination.QR -> {
                    Spacer(Modifier.weight(1f))
                    BotonOpciones("Screen Opciones")
                }
                Destination.MONUMENTOS -> {
                        Text("Mis puntos: 5")
                        Column(){
                            Text("Lugares Escaneados")
                            Text("5 de 15")
                        }
                }
                Destination.CONFIGURACION -> {
                    Spacer(Modifier.weight(1f))
                    SwitchModoOscuro()
                }
            }
        }
    }
}

