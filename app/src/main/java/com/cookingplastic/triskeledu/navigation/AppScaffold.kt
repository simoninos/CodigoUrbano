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
import com.cookingplastic.triskeledu.ui.components.BarraSinConexion
import com.cookingplastic.triskeledu.ui.components.DialogoSubisteNivel
import com.cookingplastic.triskeledu.ui.shared.RolViewModel
import com.cookingplastic.triskeledu.ui.shared.SubidaNivelViewModel

// Scaffold central de la app: dibuja la barra de navegación inferior en las tres pestañas y en el Detalle.
// Cada pantalla dibuja su propia cabecera, como en el mockup.
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    //EXC-06: el servidor no responde; se muestra la barra superior "Sin conexión"
    sinConexion: Boolean = false,
    subidaNivelViewModel: SubidaNivelViewModel = viewModel(factory = SubidaNivelViewModel.Factory),
    rolViewModel: RolViewModel = viewModel(factory = RolViewModel.Factory)
) {
    val navController = rememberNavController()
    val rol by rolViewModel.rol.collectAsStateWithLifecycle()
    val solicitudesPendientes by rolViewModel.solicitudesPendientes.collectAsStateWithLifecycle()
    val notificacionesSinLeer by rolViewModel.notificacionesSinLeer.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route
    // El Detalle mantiene la barra inferior, pero sin ninguna pestaña marcada (pestanaMarcada)
    val mostrarBarra = Destination.esPestana(rutaActual) || rutaActual == Rutas.DETALLE
    val marcada = pestanaMarcada(rutaActual)

    Scaffold(
        modifier = modifier,
        topBar = { BarraSinConexion(visible = sinConexion) },
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
                        val seleccionada = destino == marcada
                        NavigationBarItem(
                            selected = seleccionada,
                            onClick = { navController.irAPestana(destino.route) },
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
