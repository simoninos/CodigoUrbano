package com.cookingplastic.triskeledu.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.ui.screens.admin.AdminEditarScreen
import com.cookingplastic.triskeledu.ui.screens.bienvenida.BienvenidaScreen
import com.cookingplastic.triskeledu.ui.screens.configuracion.ConfiguracionScreen
import com.cookingplastic.triskeledu.ui.screens.creador.AsociarCapsulaScreen
import com.cookingplastic.triskeledu.ui.screens.creador.CapsulaDetalleScreen
import com.cookingplastic.triskeledu.ui.screens.creador.CapsulaEditarScreen
import com.cookingplastic.triskeledu.ui.screens.creador.CapsulaPreviaScreen
import com.cookingplastic.triskeledu.ui.screens.creador.CapsulasScreen
import com.cookingplastic.triskeledu.ui.screens.detalle.DetalleScreen
import com.cookingplastic.triskeledu.ui.screens.escanear.EscanearScreen
import com.cookingplastic.triskeledu.ui.screens.login.LoginScreen
import com.cookingplastic.triskeledu.ui.screens.login.RecuperarScreen
import com.cookingplastic.triskeledu.ui.screens.login.RegistroScreen
import com.cookingplastic.triskeledu.ui.screens.login.SolicitudRevisionScreen
import com.cookingplastic.triskeledu.ui.screens.mapa.MapaScreen
import com.cookingplastic.triskeledu.ui.screens.mislugares.MisLugaresAdminScreen
import com.cookingplastic.triskeledu.ui.screens.mislugares.MisLugaresScreen
import com.cookingplastic.triskeledu.ui.screens.nivel.NivelScreen
import com.cookingplastic.triskeledu.ui.screens.notificaciones.NotificacionesScreen
import com.cookingplastic.triskeledu.ui.state.DestinoNotificacion
import com.cookingplastic.triskeledu.ui.screens.perfil.PerfilScreen
import com.cookingplastic.triskeledu.ui.screens.ranking.RankingScreen
import com.cookingplastic.triskeledu.ui.screens.selfie.SelfieScreen
import com.cookingplastic.triskeledu.ui.screens.solicitudes.SolicitudesScreen
import com.cookingplastic.triskeledu.ui.screens.video.VideoScreen

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

// Evita que dos toques rápidos al engranaje apilen Configuración dos veces
private fun NavHostController.abrirConfiguracion() {
    navigate(Rutas.CONFIGURACION) { launchSingleTop = true }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    // MAP-01 y LUG-05: con rol ADMINISTRADOR la pestaña Mis lugares es P07-Admin
    rol: RolUsuario? = null,
    solicitudesPendientes: Int = 0,
    //PER-03: el contador de la campana de Mis lugares
    notificacionesSinLeer: Int = 0
) {
    // ACC-01: la app abre en la bienvenida; si ya hay sesión, la bienvenida pasa directo al mapa
    NavHost(
        navController = navController,
        startDestination = Rutas.BIENVENIDA,
        modifier = modifier,
        enterTransition = { entrar() },
        exitTransition = { salir() },
        popEnterTransition = { volverEntrar() },
        popExitTransition = { volverSalir() }
    ) {
        // ---------- Sesión ----------
        composable(Rutas.BIENVENIDA) {
            BienvenidaScreen(
                alCrearCuenta = { navController.navigate(Rutas.REGISTRO) },
                alTengoCuenta = { navController.navigate(Rutas.LOGIN) },
                alSesionIniciada = { irAlMapa(navController) }
            )
        }
        composable(Rutas.LOGIN) {
            LoginScreen(
                alIniciarSesion = { irAlMapa(navController) },
                alOlvidarClave = { navController.navigate(Rutas.RECUPERAR) },
                alCrearCuenta = { navController.navigate(Rutas.REGISTRO) },
                alSolicitudPendiente = { navController.navigate(Rutas.SOLICITUD_REVISION) }
            )
        }
        composable(Rutas.REGISTRO) {
            RegistroScreen(
                // Se registra e inicia sesión al tiro: va directo al mapa
                alRegistrarse = { irAlMapa(navController) },
                // ACC-07: pidió ser creador; atrás vuelve a la bienvenida, no al formulario
                alSolicitarCreador = {
                    navController.navigate(Rutas.SOLICITUD_REVISION) {
                        popUpTo(Rutas.BIENVENIDA)
                    }
                },
                // "Ya tengo cuenta": cambia el registro por el login
                alVolver = {
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(Rutas.REGISTRO) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Rutas.RECUPERAR) {
            RecuperarScreen(
                alVolver = { navController.popBackStack() },
                alIrAlLogin = { navController.popBackStack() }
            )
        }
        composable(Rutas.SOLICITUD_REVISION) {
            SolicitudRevisionScreen(alVolverAlInicio = { irABienvenida(navController) })
        }

        // ---------- Pestañas ----------
        composable(Destination.MAPA.route) {
            MapaScreen(
                alVerDetalle = { puntoId -> navController.navigate(Rutas.detalle(puntoId)) },
                alAbrirConfiguracion = { navController.abrirConfiguracion() },
                alVerNivel = { navController.navigate(Rutas.NIVEL) },
                alAsociarCapsula = { puntoId -> navController.navigate(Rutas.asociarCapsula(puntoId)) },
                alEditarPunto = { puntoId -> navController.navigate(Rutas.adminEditar(puntoId)) },
                alAbrirNotificaciones = { navController.navigate(Rutas.NOTIFICACIONES) }
            )
        }
        composable(Destination.ESCANEAR.route) {
            // Lugar encontrado (pantalla 6) se muestra dentro de Escanear; desde ahí se abre el detalle
            EscanearScreen(
                alAbrirLugar = { puntoId -> navController.navigate(Rutas.detalle(puntoId)) }
            )
        }
        composable(Destination.MIS_LUGARES.route) {
            if (rol == RolUsuario.ADMINISTRADOR) {
                MisLugaresAdminScreen(
                    alNuevoPunto = { navController.navigate(Rutas.adminEditar()) },
                    alEditar = { puntoId -> navController.navigate(Rutas.adminEditar(puntoId)) },
                    alAbrirConfiguracion = { navController.abrirConfiguracion() },
                    alAbrirNotificaciones = { navController.navigate(Rutas.NOTIFICACIONES) },
                    notificacionesSinLeer = notificacionesSinLeer
                )
                return@composable
            }
            MisLugaresScreen(
                alVerDetalle = { puntoId -> navController.navigate(Rutas.detalle(puntoId)) },
                alAbrirConfiguracion = { navController.abrirConfiguracion() },
                alAbrirNotificaciones = { navController.navigate(Rutas.NOTIFICACIONES) },
                notificacionesSinLeer = notificacionesSinLeer,
                alVerCapsula = { capsulaId, puntoId -> navController.navigate(Rutas.video(capsulaId, puntoId)) },
                //ver en mapa: cambia a la pestaña Mapa igual que la barra inferior; el punto lo centra PedidoMapa
                alIrAlMapa = {
                    navController.navigate(Destination.MAPA.route) {
                        popUpTo(Destination.MAPA.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                //LUG-06: "Ver ranking ›" abre P20
                alVerRanking = { navController.navigate(Rutas.RANKING) }
            )
        }

        // ---------- Pantallas internas ----------
        composable(Rutas.RANKING) {
            RankingScreen(alVolver = { navController.popBackStack() })
        }
        composable(Rutas.NIVEL) {
            NivelScreen(
                alVolver = { navController.popBackStack() },
                //LVL-01: "Cambiar tema" abre Configuración, donde está Apariencia
                alCambiarTema = { navController.abrirConfiguracion() }
            )
        }
        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_PUNTO_ID) { type = NavType.StringType })
        ) {
            // DetalleViewModel lee el puntoId de la ruta
            DetalleScreen(
                alVolver = { navController.popBackStack() },
                alAbrirConfiguracion = { navController.abrirConfiguracion() },
                alVerCapsula = { capsulaId, puntoId -> navController.navigate(Rutas.video(capsulaId, puntoId)) },
                alTomarSelfie = { puntoId -> navController.navigate(Rutas.selfie(puntoId)) }
            )
        }
        composable(
            route = Rutas.SELFIE,
            arguments = listOf(navArgument(Rutas.ARG_PUNTO_ID) { type = NavType.StringType })
        ) {
            SelfieScreen(alVolver = { navController.popBackStack() })
        }

        // P12: VideoViewModel lee la cápsula, el punto y si es vista previa desde la ruta
        composable(
            route = Rutas.VIDEO,
            arguments = listOf(
                navArgument(Rutas.ARG_CAPSULA_ID) { type = NavType.StringType },
                navArgument(Rutas.ARG_PUNTO_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Rutas.ARG_VISTA_PREVIA) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            VideoScreen(alVolver = { navController.popBackStack() })
        }

        composable(
            route = Rutas.CONFIGURACION,
            // Configuración tiene su propia animación de entrada (el engranaje que se expande)
            enterTransition = { EnterTransition.None },
            popExitTransition = { fadeOut(tween(DURACION_MS)) },
        ) {
            ConfiguracionScreen(
                // ACC-06: vuelve a la bienvenida y se borra todo el historial
                alCerrarSesion = { irABienvenida(navController) },
                alVolver = { navController.popBackStack() },
                alAbrirMisCapsulas = { navController.navigate(Rutas.CAPSULAS) },
                alAbrirPerfil = { navController.navigate(Rutas.PERFIL) }
            )
        }
        composable(Rutas.PERFIL) {
            PerfilScreen(alVolver = { navController.popBackStack() })
        }
        composable(Rutas.NOTIFICACIONES) {
            NotificacionesScreen(
                alVolver = { navController.popBackStack() },
                //PER-03: cada notificación abre lo que tiene relacionado
                alAbrirDestino = { destino ->
                    when (destino) {
                        is DestinoNotificacion.Detalle -> navController.navigate(Rutas.detalle(destino.puntoId))
                        is DestinoNotificacion.EditarPunto -> navController.navigate(Rutas.adminEditar(destino.puntoId))
                        DestinoNotificacion.Solicitudes -> navController.navigate(Rutas.SOLICITUDES)
                        DestinoNotificacion.Nivel -> navController.navigate(Rutas.NIVEL)
                    }
                }
            )
        }

        // ---------- Creador de contenido ----------
        composable(Rutas.CAPSULAS) {
            CapsulasScreen(
                alVolver = { navController.popBackStack() },
                alNuevaCapsula = { navController.navigate(Rutas.capsulaEditar()) },
                alEditar = { capsulaId -> navController.navigate(Rutas.capsulaEditar(capsulaId)) },
                alVerDetalle = { capsulaId -> navController.navigate(Rutas.capsulaDetalle(capsulaId)) }
            )
        }
        composable(
            route = Rutas.CAPSULA_DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_CAPSULA_ID) { type = NavType.StringType })
        ) {
            CapsulaDetalleScreen(
                alVolver = { navController.popBackStack() },
                alEditar = { capsulaId -> navController.navigate(Rutas.capsulaEditar(capsulaId)) }
            )
        }
        composable(
            route = Rutas.CAPSULA_EDITAR,
            arguments = listOf(
                navArgument(Rutas.ARG_CAPSULA_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            CapsulaEditarScreen(
                alVolver = { navController.popBackStack() },
                alProbarVideo = { capsulaId -> navController.navigate(Rutas.capsulaPrevia(capsulaId)) }
            )
        }
        composable(
            route = Rutas.CAPSULA_PREVIA,
            arguments = listOf(
                navArgument(Rutas.ARG_CAPSULA_ID) { type = NavType.StringType },
                //el reproductor de P12 lee este argumento: aquí siempre es vista previa (libre y sin puntos)
                navArgument(Rutas.ARG_VISTA_PREVIA) {
                    type = NavType.BoolType
                    defaultValue = true
                }
            )
        ) {
            CapsulaPreviaScreen(
                alVolver = { navController.popBackStack() },
                //publicada: se abre su detalle y se quitan el formulario y la vista previa del historial
                alPublicada = { capsulaId ->
                    navController.navigate(Rutas.capsulaDetalle(capsulaId)) {
                        popUpTo(Rutas.CAPSULAS)
                    }
                }
            )
        }
        composable(
            route = Rutas.ASOCIAR_CAPSULA,
            arguments = listOf(navArgument(Rutas.ARG_PUNTO_ID) { type = NavType.StringType })
        ) {
            AsociarCapsulaScreen(alVolver = { navController.popBackStack() })
        }

        // ---------- Solicitudes de creador (administrador) ----------
        composable(Rutas.SOLICITUDES) {
            SolicitudesScreen(alVolver = { navController.popBackStack() })
        }

        // ---------- Administrador: P10 crear o editar punto (la lista ahora es P07-Admin) ----------

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

// ACC-04: al entrar al mapa se borra todo el historial: atrás no vuelve al login ni a la bienvenida
private fun irAlMapa(navController: NavHostController) {
    navController.navigate(Destination.MAPA.route) {
        popUpTo(navController.graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun irABienvenida(navController: NavHostController) {
    navController.navigate(Rutas.BIENVENIDA) {
        popUpTo(navController.graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
