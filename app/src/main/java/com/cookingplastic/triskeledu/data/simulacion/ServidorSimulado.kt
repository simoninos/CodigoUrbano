package com.cookingplastic.triskeledu.data.simulacion

import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.data.sincronizacion.RespuestaRegistro
import com.cookingplastic.triskeledu.data.sincronizacion.ServidorApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

//SIM-03: hace de backend en el mismo teléfono. Lo que el servidor ya aceptó son las filas con sincronizado = true,
//y los puntos son los de Room. Las revisiones están en ReglaSincronizacion
class ServidorSimulado(
    private val progresoRepository: ProgresoRepository,
    private val puntoInteresRepository: PuntoInteresRepository,
    //una pequeña espera, como si la respuesta viajara por la red
    private val latenciaMs: Long = 300
) : ServidorApi {

    override suspend fun confirmarEscaneos(usuarioId: String, escaneos: List<ProgresoPunto>): List<RespuestaRegistro> {
        delay(latenciaMs)
        val confirmados = progresoRepository.progresoDe(usuarioId).first().filter { it.sincronizado }
        return ReglaSincronizacion.revisarEscaneos(escaneos, confirmados, puntos())
    }

    override suspend fun confirmarComentarios(usuarioId: String, comentarios: List<Comentario>): List<RespuestaRegistro> {
        delay(latenciaMs)
        return ReglaSincronizacion.revisarComentarios(comentarios, puntos())
    }

    //todos los puntos, también los inactivos: un escaneo hecho antes de desactivar el punto sigue valiendo
    private suspend fun puntos() = puntoInteresRepository.obtenerTodos().first().associateBy { it.id }
}
