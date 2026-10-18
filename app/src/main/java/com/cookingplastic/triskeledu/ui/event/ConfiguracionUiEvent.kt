package com.cookingplastic.triskeledu.ui.event

//un aviso por cada interruptor (trae el true/false que manda) y uno para cerrar sesión
sealed interface ConfiguracionUiEvent {
    data class OnTemaNocheChange(val valor: Boolean) : ConfiguracionUiEvent
    data class OnNotifLugaresCercanosChange(val valor: Boolean) : ConfiguracionUiEvent
    data class OnNotifNuevosVideosChange(val valor: Boolean) : ConfiguracionUiEvent
    data class OnSeguirUbicacionChange(val valor: Boolean) : ConfiguracionUiEvent
    data class OnMostrarBloqueadosChange(val valor: Boolean) : ConfiguracionUiEvent
    data object OnCerrarSesion : ConfiguracionUiEvent
}
