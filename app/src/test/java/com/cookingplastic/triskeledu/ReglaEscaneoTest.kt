package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.ui.validation.ReglaEscaneo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

//Regla de los 100 m: se prueba justo antes, justo en el límite y justo después
class ReglaEscaneoTest {

    @Test
    fun a99MetrosSeDesbloquea() {
        assertTrue(ReglaEscaneo.puedeDesbloquear(99.9))
        assertNull(ReglaEscaneo.mensajeRechazo(99.9))
    }

    @Test
    fun a100MetrosJustoSeDesbloquea() {
        assertTrue(ReglaEscaneo.puedeDesbloquear(100.0))
    }

    @Test
    fun a101MetrosNoSeDesbloqueaYDiceLaDistancia() {
        assertFalse(ReglaEscaneo.puedeDesbloquear(100.1))
        assertEquals(
            "Estás a 120 m. Acércate a 100 m o menos para desbloquearlo",
            ReglaEscaneo.mensajeRechazo(120.0)
        )
    }

    @Test
    fun sinGpsNoSeEscanea() {
        assertFalse(ReglaEscaneo.puedeDesbloquear(null))
        assertEquals("Activa el GPS para escanear", ReglaEscaneo.mensajeRechazo(null))
    }
}