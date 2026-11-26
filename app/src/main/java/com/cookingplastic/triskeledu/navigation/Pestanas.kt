package com.cookingplastic.triskeledu.navigation

import androidx.navigation.NavController

//La pestaña marcada en la barra inferior: solo la de la pantalla actual. En el Detalle no se marca ninguna,
//porque el Detalle no es una pestaña y marcar la de origen hacía creer que se estaba en el Mapa
fun pestanaMarcada(rutaActual: String?): Destination? = Destination.entries.find { it.route == rutaActual }

//Cambia a una pestaña desde la barra inferior o desde un botón (Ver en mapa, Escanear el QR del lugar).
//Primero cierra el Detalle: si quedara, se guardaría dentro de la pestaña (saveState) y al volver a ella
//se vería el Detalle en vez de la pestaña
fun NavController.irAPestana(ruta: String) {
    while (currentDestination?.route == Rutas.DETALLE && popBackStack()) Unit
    navigate(ruta) {
        //vuelve al Mapa, base del historial después del login, guardando el estado de cada pestaña
        popUpTo(Destination.MAPA.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
