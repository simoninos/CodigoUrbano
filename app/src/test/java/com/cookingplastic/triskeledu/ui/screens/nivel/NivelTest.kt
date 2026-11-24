package com.cookingplastic.triskeledu.ui.screens.nivel

import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.domain.rules.ReglaNivel
import org.junit.Assert.assertEquals
import org.junit.Test

//LVL-01 (P24) y LVL-04: barra entre umbrales y estado de cada nivel
class NivelTest {

    @Test
    fun barraEntreLosDosUmbrales() {
        //160 puntos: nivel 3 (105) y el 4 se alcanza con 210
        val estado = armarNivel(puntos = 160, nivelMaximoAlcanzado = 3)

        assertEquals(3, estado.nivel.numero)
        assertEquals("Descubridor", estado.nivel.nombre)
        assertEquals(105, estado.nivel.puntosDesde)
        assertEquals(210, estado.nivel.puntosSiguiente)
        assertEquals(50, estado.faltanParaSiguiente)
        //(160 - 105) / (210 - 105)
        assertEquals(55f / 105f, estado.avance, 0.0001f)
    }

    @Test
    fun barraEnLosBordes() {
        //justo en el umbral la barra parte vacía; un punto antes del siguiente casi llena
        assertEquals(0f, avanceNivel(ReglaNivel.nivelDesde(210)), 0f)
        assertEquals(139f / 140f, avanceNivel(ReglaNivel.nivelDesde(349)), 0.0001f)
        assertEquals(0f, avanceNivel(ReglaNivel.nivelDesde(0)), 0f)
    }

    @Test
    fun estadoDeCadaNivel() {
        val filas = armarNivel(puntos = 160, nivelMaximoAlcanzado = 3).filas

        assertEquals(8, filas.size)
        assertEquals(listOf(EstadoFilaNivel.DESBLOQUEADO, EstadoFilaNivel.DESBLOQUEADO, EstadoFilaNivel.NIVEL_ACTUAL),
            filas.take(3).map { it.estado })
        assertEquals(EstadoFilaNivel.BLOQUEADO, filas[3].estado)
        //"Faltan X pts": 210 - 160 y 980 - 160
        assertEquals(50, filas[3].faltanPuntos)
        assertEquals(820, filas[7].faltanPuntos)
        assertEquals(TemaApp.NOCHE_MUSEO, filas[3].tema)
        assertEquals("Guía urbano", filas[3].nombre)
    }

    @Test
    fun siBajaDeNivelLosQueAlcanzoSiguenDesbloqueados() {
        //EXC-12 y RN-33: llegó al 5 y ahora tiene 160 puntos (nivel 3)
        val filas = armarNivel(puntos = 160, nivelMaximoAlcanzado = 5).filas

        assertEquals(EstadoFilaNivel.NIVEL_ACTUAL, filas[2].estado)
        assertEquals(EstadoFilaNivel.DESBLOQUEADO, filas[3].estado)
        assertEquals(EstadoFilaNivel.DESBLOQUEADO, filas[4].estado)
        assertEquals(EstadoFilaNivel.BLOQUEADO, filas[5].estado)
    }
}
