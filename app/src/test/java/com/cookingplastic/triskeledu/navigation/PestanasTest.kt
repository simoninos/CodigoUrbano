package com.cookingplastic.triskeledu.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PestanasTest {

    @Test
    fun cada_pestana_se_marca_a_si_misma() {
        Destination.entries.forEach { assertEquals(it, pestanaMarcada(it.route)) }
    }

    @Test
    fun en_el_detalle_no_se_marca_ninguna_pestana() {
        assertNull(pestanaMarcada(Rutas.DETALLE))
    }

    @Test
    fun sin_ruta_no_se_marca_ninguna_pestana() {
        assertNull(pestanaMarcada(null))
        assertNull(pestanaMarcada(Rutas.CONFIGURACION))
    }
}
