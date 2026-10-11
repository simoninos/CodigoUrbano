package com.cookingplastic.triskeledu.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cookingplastic.triskeledu.ui.screens.mapa.MapaScreen


@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Destination,
    modifier: Modifier = Modifier
) {
    NavHost(navController, startDestination = startDestination.route, modifier = modifier) {
        composable(Destination.MAPA.route) { MapaScreen() }
        composable(Destination.MONUMENTOS.route) { Text("Monumentos")}
        composable(Destination.QR.route) {Text("Pantalla de Escaneo QR")}

        composable("detalle") { Text("Pantalla de Detalle")}
    }
}



