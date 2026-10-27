package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.PUNTOS_MAXIMOS_LUGAR
import com.cookingplastic.triskeledu.data.model.PuntoInteres

data class DetalleUiState(
    val punto: PuntoInteres? = null,
    // D-21: cápsula de video asociada al punto; null si no tiene
    val capsula: Capsula? = null,
    val estadoPunto: EstadoPunto = EstadoPunto.BLOQUEADO,
    // D-15: avance del usuario en este lugar, que muestra la esfera de agua (RN-16)
    val puntosLugar: Int = 0,
    val puntosMaximos: Int = PUNTOS_MAXIMOS_LUGAR,
    val videoVisto: Boolean = false,
    // D-16 a D-19: comentarios del lugar, del más nuevo al más antiguo, y el formulario para escribir uno
    val comentarios: List<Comentario> = emptyList(),
    val usuarioId: String? = null,
    val formularioComentarioAbierto: Boolean = false,
    val textoComentario: String = "",
    val errorComentario: String? = null,
    val guardandoComentario: Boolean = false,
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

    // D-18: "Publicar" se habilita solo con texto válido y mientras no se esté guardando
    val puedePublicarComentario: Boolean
        get() = textoComentario.isNotBlank() && errorComentario == null && !guardandoComentario
}
