package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion

// Un punto tal como lo dibuja el mapa: el punto, su estado para el usuario
// y la distancia hasta él (null mientras no hay ubicación)
data class PuntoMapa(
    val punto: PuntoInteres,
    val estado: EstadoPunto,
    val distanciaMetros: Double?
)

// Posición de la cámara del mapa: centro y nivel de zoom
data class CamaraMapa(
    val latitud: Double,
    val longitud: Double,
    val zoom: Double
)

data class MapaUiState(
    val puntos: List<PuntoMapa> = emptyList(),
    val seleccionadoId: String? = null,
    val ubicacionUsuario: Ubicacion? = null,
    val nombreUsuario: String = "",
    val totalPuntos: Int = 0,
    // Opciones del mapa guardadas en Preferencias (RF-C16)
    val seguirUbicacion: Boolean = true,
    val mostrarBloqueados: Boolean = true,
    // Sube en 1 cada vez que se toca "Centrar"; el mapa reacciona al cambio
    val solicitudCentrar: Int = 0,
    // M-23: cámara al salir del mapa; al volver se abre en el mismo lugar (null = primera vez)
    val camara: CamaraMapa? = null,
    val cargando: Boolean = true,
    val mensajeError: String? = null
) {
    // Los get() se calculan en cada lectura, así nunca quedan desactualizados
    val seleccionado: PuntoMapa?
        get() = puntos.find { it.punto.id == seleccionadoId }

    // M-08: sin "mostrar bloqueados", los puntos bloqueados no se dibujan
    val puntosVisibles: List<PuntoMapa>
        get() = if (mostrarBloqueados) puntos else puntos.filter { it.estado != EstadoPunto.BLOQUEADO }

    // M-13: "N de M lugares", con M = puntos activos
    val lugaresEscaneados: Int
        get() = puntos.count { it.estado != EstadoPunto.BLOQUEADO }

    val totalLugares: Int
        get() = puntos.size
}
