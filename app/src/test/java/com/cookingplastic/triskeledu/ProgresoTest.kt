package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.estaCompletado
import com.cookingplastic.triskeledu.data.model.puntosTotales
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgresoTest {

    private val enLaMoneda = Ubicacion(-33.4429, -70.6540, 567.0)

    @Test
    fun recienEscaneadoTiene5Puntos() {
        val progreso = ProgresoPunto(usuarioId = "u1", puntoId = "p1", escaneoUbicacion = enLaMoneda)
        assertEquals(5, progreso.puntosTotales())
        assertFalse(progreso.estaCompletado())
    }

    @Test
    fun conVideoVistoQuedaCompletado() {
        val progreso = ProgresoPunto(usuarioId = "u1", puntoId = "p1", escaneoUbicacion = enLaMoneda, puntosVideo = 15)
        assertEquals(20, progreso.puntosTotales())
        assertTrue(progreso.estaCompletado())
    }

    @Test
    fun escanearDosVecesNoSumaDosVeces() = runBlocking {
        val repo = FakeProgresoRepository()
        repo.registrarEscaneo("u1", "p1", enLaMoneda)
        repo.registrarEscaneo("u1", "p1", enLaMoneda)
        repo.registrarEscaneo("u1", "p2", enLaMoneda)
        repo.registrarEscaneo("u2", "p1", enLaMoneda)

        assertEquals(10, repo.totalPuntos("u1").first())
        assertEquals(2, repo.progresoDe("u1").first().size)
        assertEquals(null, repo.progresoDePunto("u1", "p3").first())
    }
}