package com.cookingplastic.triskeledu.ui.event

import com.cookingplastic.triskeledu.data.model.TemaApp

//un aviso por cada interruptor (trae el true/false que manda), uno por el tema elegido y uno para cerrar sesión
sealed interface ConfiguracionUiEvent {
    data class OnTemaChange(val tema: TemaApp) : ConfiguracionUiEvent
    data class OnTemaNocheChange(val valor: Boolean) : ConfiguracionUiEvent
    data class OnNotifLugaresCercanosChange(val valor: Boolean) : ConfiguracionUiEvent
    data class OnNotifNuevasCapsulasChange(val valor: Boolean) : ConfiguracionUiEvent
    data class OnSeguirUbicacionChange(val valor: Boolean) : ConfiguracionUiEvent
    data class OnMostrarBloqueadosChange(val valor: Boolean) : ConfiguracionUiEvent
    //P08-Admin
    data class OnNotifSolicitudesChange(val valor: Boolean) : ConfiguracionUiEvent
    data class OnNotifPuntosListosChange(val valor: Boolean) : ConfiguracionUiEvent
    data class OnMostrarInactivosChange(val valor: Boolean) : ConfiguracionUiEvent
    data object OnCerrarSesion : ConfiguracionUiEvent
}
