package com.cookingplastic.triskeledu.ui.screens.selfie

//P13 · Selfie (ACT-03)
data class SelfieUiState(
    val nombrePunto: String = "",
    //RN-07: solo con el punto escaneado; si ya tiene selfie, no suma de nuevo
    val desbloqueado: Boolean = false,
    val yaTomada: Boolean = false,
    //EXC-02: null mientras no se sabe; false muestra por qué se necesita la cámara
    val permisoCamara: Boolean? = null,
    //tras dos rechazos Android ya no muestra el diálogo: se ofrece abrir los ajustes
    val permisoBloqueado: Boolean = false,
    val camaraFrontal: Boolean = true,
    val guardando: Boolean = false,
    //20 si los sumó ahora; la pantalla muestra el aviso y vuelve al detalle
    val puntosGanados: Int? = null,
    val mensajeError: String? = null,
    val cargando: Boolean = true
)
