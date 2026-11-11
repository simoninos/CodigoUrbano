package com.cookingplastic.triskeledu.ui.screens.ranking


sealed interface RankingUiEvent {
    //"Este mes" o "Siempre"
    data class OnPeriodoChange(val periodo: PeriodoRanking) : RankingUiEvent
}
