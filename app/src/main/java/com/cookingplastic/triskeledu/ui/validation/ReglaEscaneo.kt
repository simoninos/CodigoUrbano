package com.cookingplastic.triskeledu.ui.validation

import kotlin.math.roundToInt

//Distancia máxima para desbloquear un lugar al escanear su QR
const val RADIO_ESCANEO_METROS = 60.0

//Regla de los 60 m. Recibe la distancia ya medida (eso lo hace UbicacionRepository de Simon),
//así se puede probar sin GPS. null significa que no hay GPS: sin ubicación no se escanea
object ReglaEscaneo {

    fun puedeDesbloquear(distanciaMetros: Double?): Boolean =
        distanciaMetros != null && distanciaMetros <= RADIO_ESCANEO_METROS

    //mensaje para la pantalla cuando no se puede desbloquear; null si sí se puede
    fun mensajeRechazo(distanciaMetros: Double?): String? = when {
        distanciaMetros == null -> "Activa el GPS para escanear"
        distanciaMetros > RADIO_ESCANEO_METROS ->
            "Estás a ${distanciaMetros.roundToInt()} m. Acércate a 60 m o menos para desbloquearlo"
        else -> null
    }
}