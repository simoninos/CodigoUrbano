package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.PUNTOS_CAPSULA
import com.cookingplastic.triskeledu.data.model.PUNTOS_SELFIE
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
        //si ya lo escaneó antes, no se crea otra fila ni se suman otros 10 puntos
        val existente = filas.value.find { it.usuarioId == usuarioId && it.puntoId == puntoId }
        if (existente != null) return existente

        //los demás campos toman sus valores por defecto: fecha de ahora, 10 puntos y sincronizado = false
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

    override suspend fun registrarVistaCapsula(usuarioId: String, puntoId: String): Boolean {
        val fila = filas.value.find { it.usuarioId == usuarioId && it.puntoId == puntoId }
        //sin escanear o ya vista: no se suma
        if (fila == null || fila.capsulaVistaEn != null) return false
        val vista = fila.copy(capsulaVistaEn = System.currentTimeMillis(), puntosCapsula = PUNTOS_CAPSULA, sincronizado = false)
        filas.update { lista -> lista.map { if (it.id == fila.id) vista else it } }
        return true
    }

    override fun vistasCompletas(puntoId: String): Flow<Int> =
        filas.map { lista -> lista.count { it.puntoId == puntoId && it.puntosCapsula == PUNTOS_CAPSULA } }

    override fun escaneosPorPunto(): Flow<Map<String, Int>> =
        filas.map { lista -> lista.groupingBy { it.puntoId }.eachCount() }

    override suspend fun registrarSelfie(usuarioId: String, puntoId: String, selfieUri: String): Boolean {
        val fila = filas.value.find { it.usuarioId == usuarioId && it.puntoId == puntoId }
        //sin escanear o con selfie: no se suma
        if (fila == null || fila.selfieEn != null) return false
        val conSelfie = fila.copy(selfieUri = selfieUri, selfieEn = System.currentTimeMillis(), puntosSelfie = PUNTOS_SELFIE, sincronizado = false)
        filas.update { lista -> lista.map { if (it.id == fila.id) conSelfie else it } }
        return true
    }

    override suspend fun pendientesDe(usuarioId: String): List<ProgresoPunto> =
        filas.value.filter { it.usuarioId == usuarioId && !it.sincronizado }

    override suspend fun marcarSincronizados(ids: List<String>) {
        filas.update { lista -> lista.map { if (it.id in ids) it.copy(sincronizado = true) else it } }
    }

    override suspend fun quitar(id: String) {
        filas.update { lista -> lista.filterNot { it.id == id } }
    }

    override fun todas(): Flow<List<ProgresoPunto>> = filas
}
