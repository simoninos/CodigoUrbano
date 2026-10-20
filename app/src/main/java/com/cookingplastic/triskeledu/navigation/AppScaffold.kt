package com.cookingplastic.triskeledu.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// Scaffold central de la app: dibuja la barra de navegación inferior solo en las tres pestañas.
// Cada pantalla dibuja su propia cabecera, como en el mockup.
@Composable
fun AppScaffold(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route
    val esPestana = Destination.esPestana(rutaActual)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            // La barra entra deslizándose desde abajo al llegar a una pestaña
            AnimatedVisibility(
                visible = esPestana,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar {
                    Destination.entries.forEach { destino ->
                        val seleccionada = rutaActual == destino.route
                        NavigationBarItem(
                            selected = seleccionada,
                            onClick = {
                                navController.navigate(destino.route) {
                                    // Vuelve al Mapa, base del historial después del login, guardando
                                    // el estado de cada pestaña para no apilar pantallas al cambiar
                                    popUpTo(Destination.MAPA.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (seleccionada) destino.iconSelected else destino.icon,
                                    contentDescription = destino.contentDescription
                                )
                            },
                            label = { Text(destino.label) }
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        AppNavHost(navController, Modifier.padding(contentPadding))
    }
}
