package com.cookingplastic.triskeledu.ui.screens.creador

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.CategoriaCapsula

//Filtro por estado de P17 (CRE-04)
enum class FiltroEstadoCapsula(val texto: String) {
    TODAS("Todas"),
    BORRADORES("Borradores"),
    PUBLICADAS("Publicadas")
}

//Una cápsula en la lista, con el punto al que está asociada (null si no tiene)
data class FilaCapsula(
    val capsula: Capsula,
    val nombrePunto: String?
)

//P17 · Mis cápsulas
data class CapsulasUiState(
    val busqueda: String = "",
    val filtroEstado: FiltroEstadoCapsula = FiltroEstadoCapsula.TODAS,
    //null = todas las categorías
    val categoria: CategoriaCapsula? = null,
    //ya filtradas y ordenadas
    val capsulas: List<FilaCapsula> = emptyList(),
    //todas las del creador, sin filtros: distingue "no tienes cápsulas" de "nada con estos filtros"
    val totalPropias: Int = 0,
    val cargando: Boolean = true
)
