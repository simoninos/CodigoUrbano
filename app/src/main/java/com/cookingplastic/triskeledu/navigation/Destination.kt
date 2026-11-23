package com.cookingplastic.triskeledu.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector
import com.cookingplastic.triskeledu.data.model.RolUsuario

/*
* Destination corresponde a las pestañas del NavigationBar: Mapa, Escanear y Mis descubrimientos.
* Cada pestaña contiene su ruta, el texto, el ícono normal, el ícono seleccionado y una descripción.
* Las pantallas que no son pestañas están en Rutas.kt.
*/
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val contentDescription: String
) {
    MAPA("mapa", "Mapa", Icons.Outlined.Map, Icons.Filled.Map, "Mapa"),
    ESCANEAR("escanear", "Escanear", Icons.Outlined.QrCodeScanner, Icons.Filled.QrCodeScanner, "Escanear QR"),
    //P07: la ruta se mantiene; cambia el nombre visible y el ícono
    MIS_LUGARES("mis_lugares", "Mis descubrimientos", Icons.Outlined.AccountBalance, Icons.Filled.AccountBalance, "Mis descubrimientos");

    companion object {
        // true si la ruta corresponde a una de las tres pestañas
        fun esPestana(ruta: String?): Boolean = entries.any { it.route == ruta }

        // MAP-01: el administrador solo tiene Mapa y Mis lugares; cliente y creador, las tres
        fun pestanasPara(rol: RolUsuario?): List<Destination> =
            if (rol == RolUsuario.ADMINISTRADOR) listOf(MAPA, MIS_LUGARES) else entries

        // P07-Admin se llama "Mis lugares"; para los demás es "Mis descubrimientos"
        fun etiquetaPara(destino: Destination, rol: RolUsuario?): String =
            if (destino == MIS_LUGARES && rol == RolUsuario.ADMINISTRADOR) "Mis lugares" else destino.label
    }
}
