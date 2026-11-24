package com.cookingplastic.triskeledu.ui.screens.video

//Lo que pasa en el reproductor: lo avisa la pantalla, que es la que tiene el ExoPlayer
sealed interface VideoUiEvent {
    data class OnAvance(val posicionMs: Long, val duracionMs: Long) : VideoUiEvent
    //el usuario pausó o reanudó, o la app pasó a segundo plano
    data class OnReproduciendo(val valor: Boolean) : VideoUiEvent
    data object OnTerminada : VideoUiEvent
    //el reproductor no pudo seguir; esDeRed = falló la conexión y no el archivo
    data class OnErrorReproduccion(val esDeRed: Boolean) : VideoUiEvent
    data object OnReintentar : VideoUiEvent
    data object OnVerDeNuevo : VideoUiEvent
}
