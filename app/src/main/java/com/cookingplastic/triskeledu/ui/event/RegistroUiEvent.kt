package com.cookingplastic.triskeledu.ui.event

//los avisos que la pantalla de registro le manda al ViewModel
sealed interface RegistroUiEvent {
    data class OnUsernameChange(val valor: String) : RegistroUiEvent
    data class OnCorreoChange(val valor: String) : RegistroUiEvent
    data class OnClaveChange(val valor: String) : RegistroUiEvent
    data class OnRepetirClaveChange(val valor: String) : RegistroUiEvent
    //la casilla avisa true o false, igual que el alCambiar del Interruptor
    data class OnTerminosChange(val valor: Boolean) : RegistroUiEvent
    //no trae nada porque los datos ya están en el estado
    data object OnRegistrar : RegistroUiEvent
}
