package com.cookingplastic.triskeledu.ui.screens.ranking

//LUG-06: los dos periodos del ranking
enum class PeriodoRanking(val etiqueta: String) {
    ESTE_MES("Este mes"),
    SIEMPRE("Siempre")
}

//Una fila de P20: con empates, dos usuarios comparten la posición
data class FilaRanking(
    val posicion: Int,
    val usuarioId: String,
    val username: String,
    val puntos: Int,
    val esActual: Boolean
)

//P20 · Ranking (LUG-06)
data class RankingUiState(
    val periodo: PeriodoRanking = PeriodoRanking.ESTE_MES,
    //los primeros del periodo
    val primeros: List<FilaRanking> = emptyList(),
    //el usuario conectado cuando no está entre los primeros: se muestra al final, después de "···"
    val filaActual: FilaRanking? = null,
    val cargando: Boolean = true
)
