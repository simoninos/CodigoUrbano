package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres

data class DetalleUiState(
    val punto: PuntoInteres? = null,
    val estadoPunto: EstadoPunto = EstadoPunto.BLOQUEADO,
    // Distancia en vivo; null mientras no hay ubicación
    val distanciaMetros: Double? = null,
    // Pantalla 11: popup sin conexión
    val mostrarSinConexion: Boolean = false,
    // Aviso corto que aparece abajo (D-14)
    val mensaje: String? = null,
    val cargando: Boolean = true,
    val noEncontrado: Boolean = false
) {
    val desbloqueado: Boolean
        get() = estadoPunto != EstadoPunto.BLOQUEADO
}
