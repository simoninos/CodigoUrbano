package com.cookingplastic.triskeledu.ui.event

//Los dos botones de "¡Subiste de nivel!" (P25)
sealed interface SubidaNivelUiEvent {
    data object OnUsarTema : SubidaNivelUiEvent
    data object OnSeguirExplorando : SubidaNivelUiEvent
}
