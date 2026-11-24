package com.cookingplastic.triskeledu.ui.screens.mislugares


//Lo que puede pasar en Mis lugares
sealed interface MisLugaresUiEvent {
    data class OnFiltroChange(val filtro: FiltroLugares) : MisLugaresUiEvent
    data class OnOrdenChange(val orden: OrdenLugares) : MisLugaresUiEvent
    //un cuadro (QR, Cápsulas, Selfie) o el botón "Mis lugares"
    data class OnVistaChange(val vista: VistaDescubrimientos) : MisLugaresUiEvent
    //"Por descubrir": abrir el mapa en ese lugar (LUG-03)
    data class OnVerEnMapa(val puntoId: String) : MisLugaresUiEvent
    data object OnMapaAbierto : MisLugaresUiEvent
}
