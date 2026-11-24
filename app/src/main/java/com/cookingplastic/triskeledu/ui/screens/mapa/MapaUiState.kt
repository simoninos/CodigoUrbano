package com.cookingplastic.triskeledu.ui.screens.mapa

import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.NivelUsuario
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.domain.rules.RADIO_ESCANEO_METROS

// Un punto tal como lo dibuja el mapa: el punto, su estado para el usuario
// y la distancia hasta él (null mientras no hay ubicación)
data class PuntoMapa(
    val punto: PuntoInteres,
    val estado: EstadoPunto,
    val distanciaMetros: Double?,
    // RN-27: Activo para el cliente; el creador también ve los Sin cápsula (CRE-07)
    val estadoAdmin: EstadoAdminPunto = EstadoAdminPunto.ACTIVO
) {
    val sinCapsula: Boolean
        get() = estadoAdmin == EstadoAdminPunto.SIN_CAPSULA

    // ADM-11: solo el administrador los ve
    val inactivo: Boolean
        get() = estadoAdmin == EstadoAdminPunto.INACTIVO

    val activo: Boolean
        get() = estadoAdmin == EstadoAdminPunto.ACTIVO

    // MAP-03: un punto activo sin escanear a 100 m o menos se puede escanear: su marcador late
    val enRango: Boolean
        get() = estado == EstadoPunto.BLOQUEADO && activo && distanciaMetros != null && distanciaMetros <= RADIO_ESCANEO_METROS
}

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
    // null sin sesión; decide los puntos Sin cápsula (CRE-07) y la campana del administrador
    val rol: RolUsuario? = null,
    // ADM-10: solicitudes de creador por responder (solo el administrador)
    val solicitudesPendientes: Int = 0,
    // PER-03: el contador de la campana
    val notificacionesSinLeer: Int = 0,
    val totalPuntos: Int = 0,
    // LVL-04: nivel para la tarjeta; null para el administrador (LVL-05)
    val nivel: NivelUsuario? = null,
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

    // M-08: sin "mostrar bloqueados", los puntos bloqueados no se dibujan. Los Sin cápsula se ven igual:
    // el creador los necesita para asociarles una cápsula
    val puntosVisibles: List<PuntoMapa>
        get() = if (mostrarBloqueados) puntos else puntos.filter { !it.activo || it.estado != EstadoPunto.BLOQUEADO }

    // M-13: "N de M lugares", con M = puntos activos
    val lugaresEscaneados: Int
        get() = puntos.count { it.activo && it.estado != EstadoPunto.BLOQUEADO }

    val totalLugares: Int
        get() = puntos.count { it.activo }

    // ADM-11: la leyenda del administrador
    val puntosInactivos: Int
        get() = puntos.count { it.inactivo }

    // CRE-07: "N puntos sin cápsula"
    val puntosSinCapsula: Int
        get() = puntos.count { it.sinCapsula }

    val esAdministrador: Boolean
        get() = rol == RolUsuario.ADMINISTRADOR
}
