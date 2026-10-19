package com.cookingplastic.triskeledu.ui.state

//Lo que la pantalla Escanear necesita saber para dibujarse
data class EscanearUiState(
    //null = todavía no sabemos (se está preguntando), true = hay permiso, false = lo rechazaron
    val permisoCamara: Boolean? = null,
    //el texto del último QR leído; null mientras la cámara sigue buscando
    val codigoLeido: String? = null
)