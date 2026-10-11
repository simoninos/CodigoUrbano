package com.cookingplastic.triskeledu.ui.screens.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.ui.components.OpenStreetMap

@Preview
@Composable
fun MapaScreenPreview(){
    MapaScreen()
}

@Composable
fun MapaScreen(){
    OpenStreetMap(
        latitud = -33.4489,
        longitud = -70.6693,
        zoomLevel = 16.0
    )
}


@Preview
@Composable
fun MapOverlayPreview(){
    MapOverlay(true)
}

@Composable
fun MapOverlay(isLogged: Boolean){
    if (isLogged) {
        val formaBox = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        Row{
            Box(modifier = Modifier
                .padding(10.dp)
                .border(width = 3.dp, color = MaterialTheme.colorScheme.secondary, shape = formaBox)
                .background(color = MaterialTheme.colorScheme.secondary, shape = formaBox)
                .padding(10.dp)
            ){
                Column{
                    Text("Simoninos")
                    Text("Puntos: 147")
                }
            }
        }
    }
}