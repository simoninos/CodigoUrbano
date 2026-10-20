package com.cookingplastic.triskeledu.ui.event

import com.cookingplastic.triskeledu.ui.state.FiltroLugares
import com.cookingplastic.triskeledu.ui.state.OrdenLugares

//Lo que puede pasar en Mis lugares
sealed interface MisLugaresUiEvent {
    data class OnFiltroChange(val filtro: FiltroLugares) : MisLugaresUiEvent
    data class OnOrdenChange(val orden: OrdenLugares) : MisLugaresUiEvent
}