package com.cookingplastic.triskeledu.ui.event

import com.cookingplastic.triskeledu.data.model.CategoriaCapsula
import com.cookingplastic.triskeledu.ui.state.FiltroEstadoCapsula

//Búsqueda y filtros de P17 (CRE-04)
sealed interface CapsulasUiEvent {
    data class OnBusquedaChange(val texto: String) : CapsulasUiEvent
    data class OnFiltroEstadoChange(val filtro: FiltroEstadoCapsula) : CapsulasUiEvent
    data class OnCategoriaChange(val categoria: CategoriaCapsula?) : CapsulasUiEvent
}
