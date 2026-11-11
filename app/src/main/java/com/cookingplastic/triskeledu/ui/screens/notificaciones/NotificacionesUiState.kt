package com.cookingplastic.triskeledu.ui.screens.notificaciones

import com.cookingplastic.triskeledu.data.model.Notificacion

//Cómo se agrupa el historial, como en el mockup
enum class GrupoNotificacion(val titulo: String) {
    HOY("HOY"),
    ESTA_SEMANA("ESTA SEMANA"),
    ANTES("ANTES")
}

//Una notificación lista para dibujar: su grupo y la fecha corta ("11:32", "Jueves" o "28 oct")
data class FilaNotificacion(
    val notificacion: Notificacion,
    val grupo: GrupoNotificacion,
    val fecha: String
)

//Lo que se abre al tocar una notificación (PER-03)
sealed interface DestinoNotificacion {
    data class Detalle(val puntoId: String) : DestinoNotificacion
    data class EditarPunto(val puntoId: String) : DestinoNotificacion
    data object Solicitudes : DestinoNotificacion
    data object Nivel : DestinoNotificacion
}

//P16 · Historial de notificaciones (PER-03)
data class NotificacionesUiState(
    val filas: List<FilaNotificacion> = emptyList(),
    val sinLeer: Int = 0,
    //ADM-10: el administrador tiene el botón "Solicitudes de creador" con su contador
    val esAdministrador: Boolean = false,
    val solicitudesPendientes: Int = 0,
    //la pantalla navega y lo vuelve a null
    val destino: DestinoNotificacion? = null,
    val cargando: Boolean = true
)
