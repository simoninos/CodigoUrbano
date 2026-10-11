package com.cookingplastic.triskeledu.data.model

data class MonumentoUiState(
    val monumentos: List<Monumento> = emptyList(),
    val cargando: Boolean = false,
    val mensajeError: String? = ""
) {
    val totalMonumentos: Int
        get() = monumentos.size
}