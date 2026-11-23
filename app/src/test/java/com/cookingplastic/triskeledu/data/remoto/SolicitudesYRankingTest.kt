package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.simulacion.ResultadoSolicitud
import com.cookingplastic.triskeledu.data.simulacion.SolicitudesRepository
import com.cookingplastic.triskeledu.ui.state.PeriodoRanking
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException

//ADM-10 (solicitudes contra ms-usuarios) y LUG-06 (ranking de ms-progreso)
class SolicitudesYRankingTest {

    private val api = mockk<BackendApi>()
    private val local = mockk<SolicitudesRepository>(relaxed = true)
    private val dao = mockk<UsuarioDao>(relaxed = true)

    private fun pendiente(id: String) = UsuarioDto(id, "nuevo_creador", "$id@ejemplo.cl", RolUsuario.CLIENTE, "1.1", 1, 2,
        EstadoSolicitud.PENDIENTE, 3, null)

    @Test
    fun aprobar_primeroEnElServidor_yLuegoLaNotificacionLocal() = runTest {
        coEvery { api.aprobarSolicitud("u1") } returns pendiente("u1")

        val resultado = SolicitudesConBackend(local, dao, api).aprobar("u1", "admin")

        assertEquals(ResultadoSolicitud.RESUELTA, resultado)
        coVerify { local.aprobar("u1", "admin") }
    }

    @Test
    fun siOtroAdministradorYaRespondio_409_esYaRespondida_EXC09() = runTest {
        coEvery { api.rechazarSolicitud("u1") } throws ErrorBackend(409, "Esta solicitud ya fue respondida")

        val resultado = SolicitudesConBackend(local, dao, api).rechazar("u1", "admin")

        assertEquals(ResultadoSolicitud.YA_RESPONDIDA, resultado)
        coVerify(exactly = 0) { local.rechazar(any(), any()) }
    }

    @Test
    fun alAbrirP22_traeLasPendientesDelServidor_yConservaLaClaveDeLasConocidas() = runTest {
        val conocido = Usuario(id = "u2", username = "viejo", correo = "u2@ejemplo.cl", claveHash = "hash", claveSal = "sal",
            terminosVersion = "1.1", terminosAceptadosEn = 1)
        coEvery { api.solicitudesPendientes() } returns listOf(pendiente("u1"), pendiente("u2"))
        coEvery { dao.buscarPorId("u1") } returns null
        coEvery { dao.buscarPorId("u2") } returns conocido
        every { local.pendientes() } returns flowOf(emptyList())

        SolicitudesConBackend(local, dao, api).pendientes().first()

        coVerify { dao.guardarUsuario(match { it.id == "u1" && it.claveHash.isNotBlank() }) }
        coVerify { dao.guardarUsuario(match { it.id == "u2" && it.claveHash == "hash" && it.claveSal == "sal" }) }
    }

    @Test
    fun sinServidor_P22_muestraLasDelTelefono() = runTest {
        coEvery { api.solicitudesPendientes() } throws ConnectException("apagado")
        every { local.pendientes() } returns flowOf(emptyList())

        assertTrue(SolicitudesConBackend(local, dao, api).pendientes().first().isEmpty())
    }

    @Test
    fun ranking_delMes_conMiPosicionFueraDeLosPrimeros() = runTest {
        coEvery { api.ranking("MES") } returns RankingDto(
            "MES",
            listOf(FilaRankingDto(1, "a", "ana", 80), FilaRankingDto(1, "b", "beto", 80), FilaRankingDto(3, "c", null, 60)),
            miPosicion = FilaRankingDto(7, "yo", "camila", 10)
        )

        val (primeros, mia) = RankingServidor(api).ranking(PeriodoRanking.ESTE_MES, "yo", cantidad = 2)

        assertEquals(listOf("ana", "beto"), primeros.map { it.username })
        assertEquals(7, mia?.posicion)
        assertTrue(mia!!.esActual)
    }

    @Test
    fun ranking_siEstoyEntreLosPrimeros_noSeRepiteAbajo() = runTest {
        coEvery { api.ranking("SIEMPRE") } returns RankingDto(
            "SIEMPRE", listOf(FilaRankingDto(1, "yo", "camila", 80), FilaRankingDto(2, "c", null, 60)),
            miPosicion = FilaRankingDto(1, "yo", "camila", 80)
        )

        val (primeros, mia) = RankingServidor(api).ranking(PeriodoRanking.SIEMPRE, "yo", cantidad = 5)

        assertNull(mia)
        assertTrue(primeros[0].esActual)
        //sin nombre (ms-usuarios no respondió) se muestra uno genérico
        assertEquals("Usuario", primeros[1].username)
    }
}
