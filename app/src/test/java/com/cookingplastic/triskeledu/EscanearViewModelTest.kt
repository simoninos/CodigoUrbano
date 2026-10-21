package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.location.FakeUbicacionRepository
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.ui.viewmodels.ProcesadorEscaneo
import com.cookingplastic.triskeledu.ui.viewmodels.ResultadoEscaneo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

//Prueba la validación del escaneo con las Fakes de Simon (lugares y GPS) y la de progreso
class EscanearViewModelTest {

    private val progreso = FakeProgresoRepository()

    private fun procesador(dondeEstoy: Ubicacion = DatosPrueba.UBICACION_CERCA_LA_MONEDA) =
        ProcesadorEscaneo(FakePuntoInteresRepository(), FakeUbicacionRepository(dondeEstoy), progreso)

    @Test
    fun cercaDeLaMonedaSeDesbloqueaYSuma5() = runBlocking {
        val resultado = procesador().procesar("TRISKEL-PI-001", "u1")

        assertTrue(resultado is ResultadoEscaneo.Desbloqueado)
        assertEquals(5, progreso.totalPuntos("u1").first())
    }

    @Test
    fun lejosNoSeDesbloqueaNiSuma() = runBlocking {
        val resultado = procesador(DatosPrueba.UBICACION_PLAZA_DE_ARMAS).procesar("TRISKEL-PI-001", "u1")

        assertTrue(resultado is ResultadoEscaneo.Rechazado)
        assertEquals(0, progreso.totalPuntos("u1").first())
    }

    @Test
    fun enElBordeDeLos60Metros() = runBlocking {
        val dentro = procesador(DatosPrueba.UBICACION_BORDE_DENTRO).procesar("TRISKEL-PI-001", "u1")
        val fuera = procesador(DatosPrueba.UBICACION_BORDE_FUERA).procesar("TRISKEL-PI-001", "u2")

        assertTrue(dentro is ResultadoEscaneo.Desbloqueado)
        assertTrue(fuera is ResultadoEscaneo.Rechazado)
    }

    @Test
    fun qrDesconocidoOInactivoNoDesbloquea() = runBlocking {
        val desconocido = procesador().procesar("OTRO-QR", "u1")
        //TRISKEL-PI-014 es Parque Almagro, que está desactivado
        val inactivo = procesador().procesar("TRISKEL-PI-014", "u1")

        assertTrue(desconocido is ResultadoEscaneo.Rechazado)
        assertTrue(inactivo is ResultadoEscaneo.Rechazado)
    }

    @Test
    fun sinSesionNoSeGuarda() = runBlocking {
        val resultado = procesador().procesar("TRISKEL-PI-001", null)

        assertTrue(resultado is ResultadoEscaneo.Rechazado)
    }

    @Test
    fun escanearDosVecesNoSumaDeNuevo() = runBlocking {
        procesador().procesar("TRISKEL-PI-001", "u1")
        val segunda = procesador().procesar("TRISKEL-PI-001", "u1")

        assertTrue(segunda is ResultadoEscaneo.Desbloqueado && segunda.yaDescubierto)
        assertEquals(5, progreso.totalPuntos("u1").first())
    }
}