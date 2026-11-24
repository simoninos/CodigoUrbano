package com.cookingplastic.triskeledu.ui.screens.creador

import com.cookingplastic.triskeledu.data.model.EstadoCapsula

//P18-B · Vista previa y publicar (CRE-03, CRE-05). El video lo maneja VideoViewModel en modo vista previa
data class CapsulaPreviaUiState(
    val estadoCapsula: EstadoCapsula? = null,
    val publicando: Boolean = false,
    //publicada: la pantalla abre su detalle (P17-B)
    val publicada: Boolean = false,
    val mensajeError: String? = null
) {
    //solo un borrador se publica; una ya publicada se puede revisar pero no "publicar de nuevo"
    val esBorrador: Boolean
        get() = estadoCapsula == EstadoCapsula.BORRADOR
}
