package com.cookingplastic.triskeledu.ui.screens.creador

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.PuntoInteres

//Una cápsula del creador en P21: motivo = null si se puede elegir; si no, por qué está deshabilitada
data class OpcionCapsula(
    val capsula: Capsula,
    val motivo: String?
) {
    val elegible: Boolean
        get() = motivo == null
}

//P21 · Asociar cápsula a un punto (CRE-08)
data class AsociarCapsulaUiState(
    val punto: PuntoInteres? = null,
    //RN-28: si el punto ya no está Sin cápsula, no se puede asociar nada
    val motivoPunto: String? = null,
    val opciones: List<OpcionCapsula> = emptyList(),
    val elegidaId: String? = null,
    val asociando: Boolean = false,
    //asociada: la pantalla vuelve al mapa
    val asociada: Boolean = false,
    val mensajeError: String? = null,
    val cargando: Boolean = true
) {
    val puedeAsociar: Boolean
        get() = motivoPunto == null && opciones.any { it.capsula.id == elegidaId && it.elegible } && !asociando
}
