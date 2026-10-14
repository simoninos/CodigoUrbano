package com.cookingplastic.triskeledu.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cookingplastic.triskeledu.ui.components.SimpleTopBar
import com.cookingplastic.triskeledu.ui.viewmodels.MonumentoViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.UsuarioViewModel


// El objetivo de esta clase es tener un AppScaffold que se dibuje como debe ser en todos los apartados
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    viewModelUsuario: UsuarioViewModel = viewModel(),
    viewModelMonumento: MonumentoViewModel = viewModel()
){
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val estadoUsuario by viewModelUsuario.estado.collectAsStateWithLifecycle()
    val estadoMonumento by viewModelMonumento.estado.collectAsStateWithLifecycle()

    // ¿La pantalla actual es una pestaña?
    val currentDestination = Destination.entries.find { it.route == currentRoute }
    val isTab = Destination.entries.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (currentDestination!=null) {
                SimpleTopBar(destination = currentDestination, estadoUsuario = estadoUsuario, estadoMonumento = estadoMonumento)
            }
        },
        bottomBar = {
            if (isTab){
                NavigationBar{
                    Destination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route

                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.iconSelected else destination.icon
                                    ,
                                    contentDescription = destination.contentDescription) },
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
