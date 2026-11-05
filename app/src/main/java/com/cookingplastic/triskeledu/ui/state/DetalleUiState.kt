package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.PUNTOS_MAXIMOS_LUGAR
import com.cookingplastic.triskeledu.data.model.PuntoInteres

data class DetalleUiState(
    val punto: PuntoInteres? = null,
    // D-21: cápsula asociada al punto; null si no tiene
    val capsula: Capsula? = null,
    val estadoPunto: EstadoPunto = EstadoPunto.BLOQUEADO,
    // D-15: avance del usuario en este lugar, que muestra la esfera de agua (RN-16)
    val puntosLugar: Int = 0,
    val puntosMaximos: Int = PUNTOS_MAXIMOS_LUGAR,
    val capsulaVista: Boolean = false,
    // marca la actividad de la selfie como hecha (ACT-03)
    val selfieTomada: Boolean = false,
    // D-16 a D-19: comentarios del lugar, del más nuevo al más antiguo, y el formulario para escribir uno
    val comentarios: List<Comentario> = emptyList(),
    val usuarioId: String? = null,
    val formularioComentarioAbierto: Boolean = false,
    val textoComentario: String = "",
    val errorComentario: String? = null,
    val guardandoComentario: Boolean = false,
    // RN-18: cambia en cada toque con el comentario inválido, para llevar el foco al campo
    val intentoComentario: Int = 0,
    // Distancia en vivo; null mientras no hay ubicación
    val distanciaMetros: Double? = null,
    // Pantalla 11: popup sin conexión
    val mostrarSinConexion: Boolean = false,
    // P12: id de la cápsula que hay que abrir; la pantalla navega y lo vuelve a null
    val capsulaPorAbrir: String? = null,
    // P13 (ACT-03): true cuando hay que abrir la cámara de la selfie
    val abrirSelfie: Boolean = false,
    // Aviso corto que aparece abajo (D-14)
    val mensaje: String? = null,
    val cargando: Boolean = true,
    val noEncontrado: Boolean = false
) {
    val desbloqueado: Boolean
        get() = estadoPunto != EstadoPunto.BLOQUEADO

    // D-18: "Publicar" se ve atenuado si el texto no es válido; ya no se deshabilita (RN-18)
    val puedePublicarComentario: Boolean
        get() = textoComentario.isNotBlank() && errorComentario == null && !guardandoComentario
}
