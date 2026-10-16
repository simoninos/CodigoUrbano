package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.Preferencias
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

//lo mismo que hicimos en el otro repository fake, es simulación
class FakePreferenciasRepository : PreferenciasRepository {

    private val actuales = MutableStateFlow(Preferencias(usuarioId = "usuario_prueba"))

    override fun preferencias(): Flow<Preferencias> = actuales

    override suspend fun guardar(preferencias: Preferencias) {
        actuales.value = preferencias
    }
}
