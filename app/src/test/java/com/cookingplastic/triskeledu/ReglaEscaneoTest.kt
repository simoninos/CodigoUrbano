package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.ui.validation.ReglaEscaneo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

//Regla de los 60 m: se prueba justo antes, justo en el límite y justo después
class ReglaEscaneoTest {

    @Test
    fun a59MetrosSeDesbloquea() {
        assertTrue(ReglaEscaneo.puedeDesbloquear(59.9))
        assertNull(ReglaEscaneo.mensajeRechazo(59.9))
    }

    @Test
    fun a60MetrosJustoSeDesbloquea() {
        assertTrue(ReglaEscaneo.puedeDesbloquear(60.0))
    }

    @Test
    fun a61MetrosNoSeDesbloqueaYDiceLaDistancia() {
        assertFalse(ReglaEscaneo.puedeDesbloquear(60.1))
        assertEquals(
            "Estás a 120 m. Acércate a 60 m o menos para desbloquearlo",
            ReglaEscaneo.mensajeRechazo(120.0)
        )
    }

    @Test
    fun sinGpsNoSeEscanea() {
        assertFalse(ReglaEscaneo.puedeDesbloquear(null))
        assertEquals("Activa el GPS para escanear", ReglaEscaneo.mensajeRechazo(null))
    }
}