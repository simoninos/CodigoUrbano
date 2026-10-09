package com.cookingplastic.triskeledu.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Castle
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable



/*
* Destination Corresponde a las referencias del NavigationBar
* Aquí esta contenida las rutas, descripciones, iconos, vectores y una descripción
*
* */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val contentDescription: String
) {
    MAPA("mapa", "Inicio", Icons.Outlined.Map, Icons.Filled.Map,"Inicio"),
    QR("qr", "Buscar", Icons.Outlined.QrCode, Icons.Filled.QrCode, "Qr"),
    MONUMENTOS("monumentos", "Monumentos", Icons.Outlined.Castle, Icons.Filled.Castle, "Monumentos"),
    CONFIGURACION("configuracion", "Configuracion", Icons.Outlined.Settings, Icons.Filled.Settings, "Configuracion")
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Destination,
    modifier: Modifier = Modifier
) {
    NavHost(navController, startDestination = startDestination.route, modifier = modifier) {
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                // Reemplaza esto por tu pantalla real, ej: InicioScreen()
                Text("Pantalla: ${destination.label}")
            }
        }

        composable("detalle") { Text("Pantalla de Detalle")}
    }
}



