package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.Capsula
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

//otra simulación
class FakeCapsulaRepository : CapsulaRepository {

    //El Simon entrego un archivo de simulación... No lo he revisado aún, pero puede contener algo que ayude en esto. Dejo la nota para no olvidarlo
    private val lista = MutableStateFlow(
        listOf(
            Capsula(
                id = "capsula-1",
                titulo = "¿Qué es la inteligencia artificial?",
                urlStream = "https://ejemplo.com/videos/ia-que-es.mp4",
                categoria = "Introducción",
                duracionSegundos = 120
            ),
            Capsula(
                id = "capsula-2",
                titulo = "Cómo aprende una máquina",
                urlStream = "https://ejemplo.com/videos/como-aprende.mp4",
                categoria = "Aprendizaje automático",
                duracionSegundos = 150
            ),
            Capsula(
                id = "capsula-3",
                titulo = "La IA en la ciudad",
                urlStream = "https://ejemplo.com/videos/ia-en-la-ciudad.mp4",
                categoria = "IA en la vida diaria",
                duracionSegundos = 180
            )
        )
    )

    override fun capsulas(): Flow<List<Capsula>> = lista

    override suspend fun obtenerPorId(id: String): Capsula? = lista.value.find { it.id == id }
}
