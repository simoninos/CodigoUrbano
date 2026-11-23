package com.cookingplastic.triskeledu.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

//LUG-03: "Por descubrir" abre el mapa en un lugar. El mapa es una pestaña y su ruta no recibe datos,
//así que el pedido queda aquí (una sola instancia, en AppContainer) y MapaViewModel lo atiende
class PedidoMapa {
    private val _puntoId = MutableStateFlow<String?>(null)
    val puntoId: StateFlow<String?> = _puntoId.asStateFlow()

    fun centrarEn(puntoId: String) {
        _puntoId.value = puntoId
    }

    fun atendido() {
        _puntoId.value = null
    }
}
