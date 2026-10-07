package com.cookingplastic.triskeledu.ui.components

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
import androidx.savedstate.savedState

// El objetivo de esta clase es tener un AppScaffold que se dibuje como debe ser en todos los apartados


@Composable
fun AppScaffold(modifier: Modifier = Modifier){
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ¿La pantalla actual es una pestaña?
    val showBottomBar = Destination.entries.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar){
                NavigationBar{
                    Destination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.contentDescription) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }

        }
    ){ contentPadding ->
        AppNavHost(navController, Destination.MAPA, Modifier.padding(contentPadding))
    }
}