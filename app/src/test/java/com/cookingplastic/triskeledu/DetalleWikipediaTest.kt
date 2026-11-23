package com.cookingplastic.triskeledu

import androidx.lifecycle.SavedStateHandle
import com.cookingplastic.triskeledu.data.externo.ResumenLugar
import com.cookingplastic.triskeledu.data.externo.WikipediaRepository
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.location.FakeUbicacionRepository
import com.cookingplastic.triskeledu.data.red.FakeRedRepository
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakeComentarioRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.ui.viewmodels.DetalleViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

// API externa en el Detalle: el resumen de Wikipedia aparece si llega y, si falla la red, solo queda un aviso
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleWikipediaTest {

    private val wikipedia = mockk<WikipediaRepository>()
    private val moneda = DatosPrueba.PUNTO_LA_MONEDA

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun detalle() = DetalleViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Rutas.ARG_PUNTO_ID to moneda.id)),
        puntoInteresRepository = FakePuntoInteresRepository(),
        ubicacionRepository = FakeUbicacionRepository(),
        redRepository = FakeRedRepository(conectado = true),
        usuarioRepository = FakeUsuarioRepository(),
        progresoRepository = FakeProgresoRepository(DatosPrueba.progreso),
        comentarioRepository = FakeComentarioRepository(),
        capsulaRepository = FakeCapsulaRepository(DatosPrueba.capsulas),
        wikipediaRepository = wikipedia
    )

    @Test
    fun conRed_muestraElResumenDelEnlaceDescubreMas() {
        val resumen = ResumenLugar("Palacio de La Moneda", "Sede del presidente de Chile.", null)
        coEvery { wikipedia.resumen(moneda.descubreMasUrl!!) } returns resumen

        val estado = detalle().estado.value

        assertEquals(resumen, estado.wikipedia)
        assertFalse(estado.cargandoWikipedia)
        assertFalse(estado.wikipediaNoDisponible)
        coVerify(exactly = 1) { wikipedia.resumen(any()) }
    }

    @Test
    fun sinRed_quedaElAviso_yElRestoDelDetalleCarga() {
        coEvery { wikipedia.resumen(any()) } throws IOException("sin red")

        val estado = detalle().estado.value

        assertNull(estado.wikipedia)
        assertTrue(estado.wikipediaNoDisponible)
        assertEquals(moneda.id, estado.punto?.id)
    }
}
