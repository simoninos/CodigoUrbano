package com.cookingplastic.triskeledu.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cookingplastic.triskeledu.ui.screens.admin.AdminEditarScreen
import com.cookingplastic.triskeledu.ui.screens.admin.AdminListaScreen
import com.cookingplastic.triskeledu.ui.screens.configuracion.ConfiguracionScreen
import com.cookingplastic.triskeledu.ui.screens.detalle.DetalleScreen
import com.cookingplastic.triskeledu.ui.screens.login.LoginScreen
import com.cookingplastic.triskeledu.ui.screens.login.RecuperarScreen
import com.cookingplastic.triskeledu.ui.screens.login.RegistroScreen
import com.cookingplastic.triskeledu.ui.screens.mapa.MapaScreen

private const val DURACION_MS = 300

// Entre pestañas la transición es solo un fundido; hacia una pantalla interna, además se desliza.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.entreTabs(): Boolean =
    Destination.esPestana(initialState.destination.route) &&
        Destination.esPestana(targetState.destination.route)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.entrar(): EnterTransition =
    if (entreTabs()) fadeIn(tween(DURACION_MS))
    else fadeIn(tween(DURACION_MS)) + slideIntoContainer(SlideDirection.Start, tween(DURACION_MS)) { it / 4 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.salir(): ExitTransition =
    fadeOut(tween(DURACION_MS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.volverEntrar(): EnterTransition =
    fadeIn(tween(DURACION_MS))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.volverSalir(): ExitTransition =
    if (entreTabs()) fadeOut(tween(DURACION_MS))
    else fadeOut(tween(DURACION_MS)) + slideOutOfContainer(SlideDirection.End, tween(DURACION_MS)) { it / 4 }

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // La app abre en el login
    NavHost(
        navController = navController,
        startDestination = Rutas.LOGIN,
        modifier = modifier,
        enterTransition = { entrar() },
        exitTransition = { salir() },
        popEnterTransition = { volverEntrar() },
        popExitTransition = { volverSalir() }
    ) {
        // ---------- Sesión ----------
        composable(Rutas.LOGIN) {
            LoginScreen(
                alIniciarSesion = {
                    // Se quita el login del historial: el botón atrás no vuelve a él
                    navController.navigate(Destination.MAPA.route) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                alOlvidarClave = { navController.navigate(Rutas.RECUPERAR) },
                alCrearCuenta = { navController.navigate(Rutas.REGISTRO) }
            )
        }
        composable(Rutas.REGISTRO) {
            // Al crear la cuenta se vuelve al login para iniciar sesión con ella
            RegistroScreen(
                alRegistrarse = { navController.popBackStack(Rutas.LOGIN, inclusive = false) },
                alVolver = { navController.popBackStack() }
            )
        }
        composable(Rutas.RECUPERAR) {
            RecuperarScreen(alVolver = { navController.popBackStack() })
        }

        // ---------- Pestañas ----------
        composable(Destination.MAPA.route) {
            MapaScreen(
                alVerDetalle = { puntoId -> navController.navigate(Rutas.detalle(puntoId)) },
                alAbrirConfiguracion = { navController.navigate(Rutas.CONFIGURACION) }
            )
        }
        composable(Destination.ESCANEAR.route) { PantallaVacia("Escanear QR") }
        composable(Destination.MIS_LUGARES.route) { PantallaVacia("Mis lugares") }

        // ---------- Pantallas internas ----------
        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_PUNTO_ID) { type = NavType.StringType })
        ) {
            // DetalleViewModel lee el puntoId de la ruta
            DetalleScreen(
                alVolver = { navController.popBackStack() },
                alAbrirConfiguracion = { navController.navigate(Rutas.CONFIGURACION) }
            )
        }

        composable(
            route = Rutas.ENCONTRADO,
            arguments = listOf(navArgument(Rutas.ARG_PUNTO_ID) { type = NavType.StringType })
        ) { PantallaVacia("Lugar encontrado") }

        composable(
            route = Rutas.CONFIGURACION,
            // Configuración tiene su propia animación de entrada (el engranaje que se expande)
            enterTransition = { EnterTransition.None },
            popExitTransition = { fadeOut(tween(DURACION_MS)) }
        ) {
            ConfiguracionScreen(
                alCerrarSesion = {
                    // Se borra todo el historial: después de cerrar sesión no se puede volver atrás
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                alAbrirAdmin = { navController.navigate(Rutas.ADMIN_LISTA) }
            )
        }

        // ---------- Modo administrador ----------
        composable(Rutas.ADMIN_LISTA) {
            AdminListaScreen(
                // "Salir" vuelve a Configuración, desde donde se entró
                alSalir = { navController.popBackStack() },
                alCrear = { navController.navigate(Rutas.adminEditar()) },
                alEditar = { puntoId -> navController.navigate(Rutas.adminEditar(puntoId)) }
            )
        }

        composable(
            route = Rutas.ADMIN_EDITAR,
            arguments = listOf(
                navArgument(Rutas.ARG_PUNTO_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            AdminEditarScreen(alVolver = { navController.popBackStack() })
        }
    }
}

// Contenido provisorio de una ruta cuya pantalla todavía no está implementada
@Composable
private fun PantallaVacia(nombre: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = nombre, style = MaterialTheme.typography.headlineSmall)
    }
}
