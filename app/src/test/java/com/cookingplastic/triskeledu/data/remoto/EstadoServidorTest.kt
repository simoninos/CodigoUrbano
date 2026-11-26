package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

//EXC-06: el estado de la conexión con el servidor y lo que no se puede hacer sin ella
class EstadoServidorTest {

    @Test
    fun revisarAhora_guardaLoQueRespondio() = runTest {
        var responde = false
        val estado = EstadoServidor(probar = { responde })

        assertFalse(estado.revisarAhora())
        assertFalse(estado.conectado.value)
        responde = true
        assertTrue(estado.revisarAhora())
        assertTrue(estado.conectado.value)
    }

    @Test
    fun cadaLlamadaAlBackend_informaSiHuboRespuesta() = runTest {
        val estado = EstadoServidor(probar = { true })
        val servidor = MockWebServer().apply { enqueue(MockResponse().setBody("[]")); start() }
        val api = ClienteBackend.crear(servidor.url("/").toString(), token = { null }, alVencer = {}, estado = estado)

        estado.informar(false)
        llamar { api.solicitudesPendientes() }
        assertTrue(estado.conectado.value)

        servidor.shutdown()
        try {
            llamar { api.solicitudesPendientes() }
            fail("el servidor está apagado")
        } catch (e: SinServidor) {
            assertFalse(estado.conectado.value)
        }
    }

    @Test
    fun guardarUnPuntoSinCambios_sinConexion_noSeGuarda() = runTest {
        val local = FakePuntoInteresRepository()
        val repo = PuntosConBackend(local, mockk(), EstadoServidor(probar = { false }))
        val moneda = local.obtenerPorId(DatosPrueba.PUNTO_LA_MONEDA.id).first()!!

        try {
            repo.guardar(moneda)
            fail("sin conexión no se edita")
        } catch (e: SinServidor) {
            assertEquals(EstadoServidor.SIN_CONEXION_EDITAR, e.message)
        }
    }

    @Test
    fun elAdministradorQuitaLaCapsula_vaAlServidor() = runTest {
        val local = FakePuntoInteresRepository()
        val moneda = local.obtenerPorId(DatosPrueba.PUNTO_LA_MONEDA.id).first()!!
        val api = mockk<BackendApi>()
        val sinCapsula = moneda.copy(capsulaId = null, activo = false)
        coEvery { api.editarPunto(moneda.id, any()) } returns PuntoDto(
            sinCapsula.id, sinCapsula.nombre, sinCapsula.descripcion, sinCapsula.tipo, sinCapsula.ubicacion.latitud,
            sinCapsula.ubicacion.longitud, sinCapsula.ubicacion.altitud, sinCapsula.imagenUri, sinCapsula.descubreMasUrl,
            sinCapsula.codigoQr, null, false, 5
        )
        val repo = PuntosConBackend(local, api, EstadoServidor(probar = { true }), esAdministrador = { true })

        repo.guardar(sinCapsula)

        coVerify { api.editarPunto(moneda.id, match { it.capsulaId == null }) }
        assertNull(local.obtenerPorId(moneda.id).first()?.capsulaId)
    }

    @Test
    fun guardarUnaCapsulaSinCambios_sinConexion_noSeGuarda() = runTest {
        val capsula: Capsula = DatosPrueba.CAPSULA_LA_MONEDA
        val repo = CapsulasConBackend(FakeCapsulaRepository(listOf(capsula)), mockk(), EstadoServidor(probar = { false }))

        try {
            repo.guardar(capsula)
            fail("sin conexión no se edita")
        } catch (e: SinServidor) {
            assertEquals(EstadoServidor.SIN_CONEXION_EDITAR, e.message)
        }
    }
}
