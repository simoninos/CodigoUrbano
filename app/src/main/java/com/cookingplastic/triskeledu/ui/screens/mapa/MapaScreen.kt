package com.cookingplastic.triskeledu.ui.screens.mapa

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview


@Preview
@Composable
fun MapOverlayPreview(){
    MapOverlay(true)
}

@Composable
fun MapOverlay(isLogged: Boolean){
    if (isLogged) {
        Surface(color = Color.Blue){
            Column{
                Text("Nombre de Usuario")
                Text("Puntos: ")
            }
        }
    }
}