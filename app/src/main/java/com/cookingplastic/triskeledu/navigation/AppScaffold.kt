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
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cookingplastic.triskeledu.ui.components.DialogoSubisteNivel
import com.cookingplastic.triskeledu.ui.viewmodels.RolViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.SubidaNivelViewModel

// Scaffold central de la app: dibuja la barra de navegación inferior en las tres pestañas y en el Detalle.
// Cada pantalla dibuja su propia cabecera, como en el mockup.
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    subidaNivelViewModel: SubidaNivelViewModel = viewModel(factory = SubidaNivelViewModel.Factory),
    rolViewModel: RolViewModel = viewModel(factory = RolViewModel.Factory)
) {
    val navController = rememberNavController()
    val rol by rolViewModel.rol.collectAsStateWithLifecycle()
    val solicitudesPendientes by rolViewModel.solicitudesPendientes.collectAsStateWithLifecycle()
    val notificacionesSinLeer by rolViewModel.notificacionesSinLeer.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route
    // El Detalle mantiene la barra inferior con la pestaña desde donde se abrió seleccionada
    // (Mapa, Escanear o Mis lugares); la pestaña es la pantalla anterior en el historial
    val enDetalle = rutaActual == Rutas.DETALLE
    val pestanaDelDetalle = if (enDetalle) {
        Destination.entries.find { it.route == navController.previousBackStackEntry?.destination?.route }
            ?: Destination.MAPA
    } else {
        null
    }
    val mostrarBarra = Destination.esPestana(rutaActual) || enDetalle

    Scaffold(
        modifier = modifier,
        bottomBar = {
            // La barra entra deslizándose desde abajo al llegar a una pestaña
            AnimatedVisibility(
                visible = mostrarBarra,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar {
                    // MAP-01: el administrador no tiene la pestaña Escanear
                    Destination.pestanasPara(rol).forEach { destino ->
                        val seleccionada = rutaActual == destino.route || destino == pestanaDelDetalle
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
                                    contentDescription = if (destino == Destination.MIS_LUGARES) Destination.etiquetaPara(destino, rol)
                                    else destino.contentDescription
                                )
                            },
                            label = {
                                Text(textAlign = TextAlign.Center,text = Destination.etiquetaPara(destino, rol)) }
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        AppNavHost(
            navController,
            Modifier.padding(contentPadding),
            rol = rol,
            solicitudesPendientes = solicitudesPendientes,
            notificacionesSinLeer = notificacionesSinLeer
        )
    }

    // P25 (LVL-02): aparece sobre cualquier pantalla cuando el nivel supera el más alto alcanzado
    val subida by subidaNivelViewModel.subida.collectAsStateWithLifecycle()
    subida?.let { DialogoSubisteNivel(subida = it, onEvent = subidaNivelViewModel::onEvent) }
}
