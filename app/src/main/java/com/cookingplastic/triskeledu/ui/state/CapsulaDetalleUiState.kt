package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres

//P17-B · Detalle de la cápsula (CRE-05)
data class CapsulaDetalleUiState(
    val capsula: Capsula? = null,
    //el punto al que está asociada y su estado; null si no tiene punto
    val punto: PuntoInteres? = null,
    val estadoPunto: EstadoAdminPunto? = null,
    //usuarios que la vieron completa (filas con los 50 puntos)
    val vistasCompletas: Int = 0,
    //RN-21: por qué no se puede retirar; null si se puede
    val motivoNoRetirable: String? = null,
    val confirmarRetiro: Boolean = false,
    val retirando: Boolean = false,
    //se retiró: la pantalla vuelve a Mis cápsulas
    val retirada: Boolean = false,
    val mensajeError: String? = null,
    val noExiste: Boolean = false,
    val cargando: Boolean = true
)
