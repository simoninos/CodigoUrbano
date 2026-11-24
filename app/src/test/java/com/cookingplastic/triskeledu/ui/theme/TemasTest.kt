package com.cookingplastic.triskeledu.ui.theme

import androidx.compose.ui.graphics.Color
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.domain.rules.ReglaNivel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

//RNF-05, RN-32 y RN-33: las 16 paletas (8 temas en Día y Noche) y los temas que desbloquea cada nivel
class TemasTest {

    private val paletas: List<Pair<String, PaletaTema>> = TemaApp.entries.flatMap { tema ->
        listOf("${tema.name} Día" to paletaDe(tema, noche = false), "${tema.name} Noche" to paletaDe(tema, noche = true))
    }

    //luminancia relativa de WCAG 2.1
    private fun luminancia(color: Color): Double {
        fun canal(c: Float): Double = if (c <= 0.04045f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * canal(color.red) + 0.7152 * canal(color.green) + 0.0722 * canal(color.blue)
    }

    private fun contraste(a: Color, b: Color): Double {
        val la = luminancia(a)
        val lb = luminancia(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    //tono en grados (0 a 360) para comparar Día y Noche
    private fun tono(color: Color): Float {
        val r = color.red
        val g = color.green
        val b = color.blue
        val mayor = maxOf(r, g, b)
        val delta = mayor - minOf(r, g, b)
        if (delta == 0f) return 0f
        val h = when (mayor) {
            r -> 60 * (((g - b) / delta) % 6)
            g -> 60 * ((b - r) / delta + 2)
            else -> 60 * ((r - g) / delta + 4)
        }
        return (h + 360) % 360
    }

    @Test
    fun hayDieciseisPaletasDistintas() {
        assertEquals(16, paletas.map { it.second }.toSet().size)
    }

    @Test
    fun contrasteDeLasDieciseisPaletas() {
        paletas.forEach { (nombre, paleta) ->
            val tinta = contraste(paleta.tinta, paleta.fondo)
            val primario = contraste(paleta.sobrePrimario, paleta.primario)
            assertTrue("$nombre: Tinta sobre Fondo es $tinta", tinta >= 4.5)
            assertTrue("$nombre: Sobre primario sobre Primario es $primario", primario >= 4.5)
        }
    }

    @Test
    fun elDiaEsDeAltoContraste() {
        TemaApp.entries.forEach { tema ->
            val dia = paletaDe(tema, noche = false)
            val tinta = contraste(dia.tinta, dia.fondo)
            assertTrue("${tema.name}: Tinta sobre Fondo de día es $tinta", tinta >= 7.0)
            //la versión Día es clara y la Noche, oscura
            assertTrue(luminancia(dia.fondo) > 0.5)
            assertTrue(luminancia(paletaDe(tema, noche = true).fondo) < 0.1)
        }
    }

    @Test
    fun laContraparteConservaElTonoDePrimarioYPuntos() {
        TemaApp.entries.forEach { tema ->
            val dia = paletaDe(tema, noche = false)
            val noche = paletaDe(tema, noche = true)
            listOf("Primario" to (dia.primario to noche.primario), "Puntos" to (dia.puntos to noche.puntos))
                .forEach { (token, colores) ->
                    val diferencia = abs(tono(colores.first) - tono(colores.second)).let { min(it, 360 - it) }
                    assertTrue("${tema.name} $token: los tonos difieren $diferencia°", diferencia <= 3f)
                }
        }
    }

    @Test
    fun cadaNivelDesbloqueaSuTema() {
        //RN-32: el nivel n tiene los n primeros temas
        (1..8).forEach { nivel ->
            val temas = ReglaNivel.temasDesbloqueados(nivel, nivel, RolUsuario.CLIENTE)
            assertEquals(TemaApp.entries.take(nivel).toSet(), temas)
            assertEquals(nivel, ReglaNivel.temaDelNivel(nivel)!!.nivelRequerido)
        }
    }

    @Test
    fun alBajarDeNivelSeConservanLosTemas() {
        //RN-33: con 160 puntos es nivel 3, pero llegó al 6
        val nivel = ReglaNivel.numeroDesde(160)
        assertEquals(3, nivel)
        val temas = ReglaNivel.temasDesbloqueados(nivel, 6, RolUsuario.CLIENTE)
        assertTrue(TemaApp.BOSQUE_NATIVO in temas)
        assertEquals(6, temas.size)
        //el creador sigue las mismas reglas que el cliente
        assertEquals(temas, ReglaNivel.temasDesbloqueados(nivel, 6, RolUsuario.CREADOR))
    }
}
