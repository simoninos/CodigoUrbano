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
    val encontrado: LugarEncontrado? = null,
    //EXC-04: el QR es de un lugar que ya tenía; se avisa y se abre su detalle (P04) sin sumar
    val yaDescubierto: LugarYaDescubierto? = null
) {
    //EXC-01 y RN-05: sin permiso de ubicación o con el GPS apagado no se escanea
    val sinUbicacion: Boolean
        get() = gps == EstadoGps.SIN_PERMISO || gps == EstadoGps.APAGADO
}

data class LugarEncontrado(
    val puntoId: String,
    val nombre: String,
    val tipo: String,
    val distanciaMetros: Int
)

data class LugarYaDescubierto(
    val puntoId: String,
    val nombre: String
)

enum class EstadoGps(val texto: String) {
    SIN_PERMISO("Sin permiso de ubicación"),
    APAGADO("GPS apagado"),
    BUSCANDO("Buscando GPS…"),
    ACTIVO("GPS activo"),
    SIN_SENAL("GPS sin señal")
}