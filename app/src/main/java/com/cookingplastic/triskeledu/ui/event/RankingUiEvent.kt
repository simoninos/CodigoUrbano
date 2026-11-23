package com.cookingplastic.triskeledu.ui.event

import com.cookingplastic.triskeledu.ui.state.PeriodoRanking

sealed interface RankingUiEvent {
    //"Este mes" o "Siempre"
    data class OnPeriodoChange(val periodo: PeriodoRanking) : RankingUiEvent
}
