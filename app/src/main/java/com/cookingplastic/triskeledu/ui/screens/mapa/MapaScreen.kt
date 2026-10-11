package com.cookingplastic.triskeledu.ui.screens.mapa

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cookingplastic.triskeledu.ui.components.OpenStreetMap

@Preview
@Composable
fun MapaScreenPreview(){
    MapaScreen()
}

@Composable
fun MapaScreen(){

    OpenStreetMap(
        latitud = -70.0,
        longitud = -30.0,
        zoomLevel = 16.0
    )
}