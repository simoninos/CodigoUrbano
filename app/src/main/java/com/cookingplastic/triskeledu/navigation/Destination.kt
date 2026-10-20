package com.cookingplastic.triskeledu.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector

/*
* Destination corresponde a las pestañas del NavigationBar: Mapa, Escanear y Mis lugares.
* Cada pestaña contiene su ruta, el texto, el ícono normal, el ícono seleccionado y una descripción.
* Las pantallas que no son pestañas están en Rutas.kt.
*/
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val contentDescription: String
) {
    MAPA("mapa", "Mapa", Icons.Outlined.Map, Icons.Filled.Map, "Mapa"),
    ESCANEAR("escanear", "Escanear", Icons.Outlined.QrCodeScanner, Icons.Filled.QrCodeScanner, "Escanear QR"),
    MIS_LUGARES("mis_lugares", "Mis lugares", Icons.Outlined.Place, Icons.Filled.Place, "Mis lugares");

    companion object {
        // true si la ruta corresponde a una de las tres pestañas
        fun esPestana(ruta: String?): Boolean = entries.any { it.route == ruta }
    }
}
