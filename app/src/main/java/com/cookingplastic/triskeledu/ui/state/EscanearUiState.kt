package com.cookingplastic.triskeledu.ui.state

//Lo que la pantalla Escanear necesita saber para dibujarse
data class EscanearUiState(
    //null = todavía no sabemos (se está preguntando), true = hay permiso, false = lo rechazaron
    val permisoCamara: Boolean? = null,
    val gps: EstadoGps = EstadoGps.SIN_PERMISO,
    //el texto del último QR leído; null mientras la cámara sigue buscando
    val codigoLeido: String? = null,
    //true mientras se busca el lugar y se mide la distancia
    val procesando: Boolean = false,
    //por qué no se desbloqueó (lejos, sin GPS, QR desconocido...)
    val mensajeRechazo: String? = null,
    //el lugar desbloqueado; cuando no es null se muestra la pantalla Lugar encontrado
    val encontrado: LugarEncontrado? = null
)

data class LugarEncontrado(
    val puntoId: String,
    val nombre: String,
    val tipo: String,
    val distanciaMetros: Int,
    //true si ya lo había escaneado antes: no suma otra vez
    val yaDescubierto: Boolean
)

enum class EstadoGps(val texto: String) {
    SIN_PERMISO("Sin permiso de ubicación"),
    BUSCANDO("Buscando GPS…"),
    ACTIVO("GPS activo"),
    SIN_SENAL("GPS sin señal")
}