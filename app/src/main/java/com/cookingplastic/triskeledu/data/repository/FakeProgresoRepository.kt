package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.puntosTotales
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

//Versión de prueba: guarda el progreso en memoria hasta tener Room. Se pierde al cerrar la app
class FakeProgresoRepository(filasIniciales: List<ProgresoPunto> = emptyList()) : ProgresoRepository {

    //todas las filas de todos los usuarios; al cambiar, avisa a quien esté escuchando
    private val filas = MutableStateFlow(filasIniciales)

    //map transforma cada lista nueva: aquí deja solo las del usuario
    override fun progresoDe(usuarioId: String): Flow<List<ProgresoPunto>> =
        filas.map { lista -> lista.filter { it.usuarioId == usuarioId } }

    override fun progresoDePunto(usuarioId: String, puntoId: String): Flow<ProgresoPunto?> =
        filas.map { lista -> lista.find { it.usuarioId == usuarioId && it.puntoId == puntoId } }

    override suspend fun registrarEscaneo(usuarioId: String, puntoId: String, ubicacion: Ubicacion): ProgresoPunto {
        //si ya lo escaneó antes, no se crea otra fila ni se suman otros 5 puntos
        val existente = filas.value.find { it.usuarioId == usuarioId && it.puntoId == puntoId }
        if (existente != null) return existente

        //los demás campos toman sus valores por defecto: fecha de ahora, 5 puntos y sincronizado = false
        val nueva = ProgresoPunto(
            usuarioId = usuarioId,
            puntoId = puntoId,
            escaneoUbicacion = ubicacion
        )
        filas.update { it + nueva }
        return nueva
    }

    override fun totalPuntos(usuarioId: String): Flow<Int> =
        progresoDe(usuarioId).map { lista -> lista.sumOf { it.puntosTotales() } }
}