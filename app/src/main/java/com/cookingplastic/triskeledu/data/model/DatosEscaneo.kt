package com.cookingplastic.triskeledu.data.model

//SEG-01: la posición para escanear, con lo que dice el GPS de ella. simulada = la entrega una app de ubicación falsa
//(Location.isMock); precision = radio en metros en que Android cree que está la posición (null si no la informa)
data class LecturaGps(
    val ubicacion: Ubicacion,
    val simulada: Boolean = false,
    val precision: Double? = null
)

//SEG-01 y SEG-03: lo que se guarda del escaneo además de la ubicación, y se envía al servidor para revisarlo
data class DatosEscaneo(
    val simulada: Boolean = false,
    val precision: Double? = null,
    //el texto completo del QR leído, con la firma (TRISKEL-PI-001.xxxx)
    val codigoQr: String? = null
)
