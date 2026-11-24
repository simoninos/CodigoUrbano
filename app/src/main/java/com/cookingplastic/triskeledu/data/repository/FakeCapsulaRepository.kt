package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.Capsula
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import com.cookingplastic.triskeledu.domain.repository.CapsulaRepository

//Versión de prueba: las mismas cápsulas de DatosPrueba que llenan Room, guardadas en memoria
class FakeCapsulaRepository(capsulasIniciales: List<Capsula> = DatosPrueba.capsulas) : CapsulaRepository {

    private val lista = MutableStateFlow(capsulasIniciales)

    override fun capsulas(): Flow<List<Capsula>> = lista

    override suspend fun obtenerPorId(id: String): Capsula? = lista.value.find { it.id == id }

    override fun observar(id: String): Flow<Capsula?> = lista.map { capsulas -> capsulas.find { it.id == id } }

    override suspend fun guardar(capsula: Capsula) {
        lista.update { capsulas ->
            if (capsulas.any { it.id == capsula.id }) capsulas.map { if (it.id == capsula.id) capsula else it }
            else capsulas + capsula
        }
    }

    override suspend fun eliminar(id: String) {
        lista.update { capsulas -> capsulas.filterNot { it.id == id } }
    }
}
