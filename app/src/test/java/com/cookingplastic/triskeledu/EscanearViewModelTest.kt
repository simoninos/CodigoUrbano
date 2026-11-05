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
    fun cercaDeLaMonedaSeDesbloqueaYSuma10() = runBlocking {
        val resultado = procesador().procesar("TRISKEL-PI-001", "u1")

        assertTrue(resultado is ResultadoEscaneo.Desbloqueado)
        assertEquals(10, progreso.totalPuntos("u1").first())
    }

    @Test
    fun padreAlonsoDeOvalleEstaActivoConSuQrNuevo() = runBlocking {
        //el punto rescatado de la rama prueba usa TRISKEL-PI-016, para no chocar con Parque Almagro
        val resultado = procesador(DatosPrueba.PUNTO_PADRE_ALONSO_OVALLE.ubicacion).procesar("TRISKEL-PI-016", "u1")

        assertTrue(resultado is ResultadoEscaneo.Desbloqueado)
        assertEquals(10, progreso.totalPuntos("u1").first())
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
        //con el formato del juego pero sin punto: un lugar eliminado
        val eliminado = procesador().procesar("TRISKEL-PI-099", "u1")

        //EXC-04: los mensajes del ERS
        assertEquals(ResultadoEscaneo.Rechazado("Este código no es de Aprendo IA"), desconocido)
        assertEquals(ResultadoEscaneo.Rechazado("Este lugar no está disponible"), inactivo)
        assertEquals(ResultadoEscaneo.Rechazado("Este lugar no está disponible"), eliminado)
        assertEquals(0, progreso.totalPuntos("u1").first())
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

        //EXC-04: "Ya descubriste este lugar" y se abre su detalle
        assertEquals(ResultadoEscaneo.YaDescubierto(DatosPrueba.PUNTO_LA_MONEDA), segunda)
        assertEquals(10, progreso.totalPuntos("u1").first())
    }

    @Test
    fun yaDescubiertoSeRevisaAntesQueLaDistancia() = runBlocking {
        procesador().procesar("TRISKEL-PI-001", "u1")
        //desde lejos, un lugar que ya tiene no muestra "Estás a 712 m"
        val desdeLejos = procesador(DatosPrueba.UBICACION_PLAZA_DE_ARMAS).procesar("TRISKEL-PI-001", "u1")

        assertTrue(desdeLejos is ResultadoEscaneo.YaDescubierto)
        assertEquals(10, progreso.totalPuntos("u1").first())
    }
}