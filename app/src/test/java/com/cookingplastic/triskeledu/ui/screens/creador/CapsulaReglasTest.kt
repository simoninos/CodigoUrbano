package com.cookingplastic.triskeledu.ui.screens.creador

import com.cookingplastic.triskeledu.data.creador.GestorCapsulas
import com.cookingplastic.triskeledu.data.creador.ResultadoCapsula
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.domain.rules.ReglaCapsula
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

//CRE-05, CRE-06 y RN-21: publicar, retirar y eliminar cápsulas
@OptIn(ExperimentalCoroutinesApi::class)
class CapsulaReglasTest {

    private val ahora = 1_800_000_000_000L
    private lateinit var capsulas: FakeCapsulaRepository
    private lateinit var puntos: FakePuntoInteresRepository
    private lateinit var gestor: GestorCapsulas

    //un borrador recién creado, con su enlace de Drive
    private val borradorConEnlace = DatosPrueba.CAPSULA_BORRADOR.copy(
        id = "borrador-con-enlace",
        urlStream = "https://drive.google.com/file/d/1aBcD/view?usp=sharing"
    )

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        capsulas = FakeCapsulaRepository(DatosPrueba.capsulas + borradorConEnlace)
        puntos = FakePuntoInteresRepository(capsulaRepository = capsulas)
        gestor = GestorCapsulas(capsulas, puntos) { ahora }
    }

    @After
    fun despues() = Dispatchers.resetMain()

    private fun capsula(id: String): Capsula? = runBlocking { capsulas.obtenerPorId(id) }
    private fun punto(id: String) = runBlocking { puntos.obtenerPorId(id).first()!! }

    @Test
    fun publicarLaPasaAPublicadaYLlenaLaFecha() = runBlocking {
        assertEquals(ResultadoCapsula.Listo, gestor.publicar(borradorConEnlace.id))

        val publicada = capsula(borradorConEnlace.id)!!
        assertEquals(EstadoCapsula.PUBLICADA, publicada.estado)
        assertEquals(ahora, publicada.publicadaEn)
    }

    @Test
    fun noSePublicaSinEnlaceNiDosVeces() = runBlocking {
        //RN-26: el enlace de Drive es obligatorio para publicar
        assertEquals(ResultadoCapsula.Error("Pega un enlace de Google Drive"), gestor.publicar(DatosPrueba.CAPSULA_BORRADOR.id))
        assertEquals(EstadoCapsula.BORRADOR, capsula(DatosPrueba.CAPSULA_BORRADOR.id)!!.estado)
        assertEquals(ResultadoCapsula.Error("Esta cápsula ya está publicada"), gestor.publicar(DatosPrueba.CAPSULA_LIBRE.id))
    }

    @Test
    fun noSeRetiraDeUnPuntoActivo() = runBlocking {
        //RN-21: La Moneda está activa
        val resultado = gestor.retirar(DatosPrueba.CAPSULA_LA_MONEDA.id)

        assertEquals(ResultadoCapsula.Error(ReglaCapsula.RETIRO_BLOQUEADO), resultado)
        assertEquals(EstadoCapsula.PUBLICADA, capsula(DatosPrueba.CAPSULA_LA_MONEDA.id)!!.estado)
        assertEquals(DatosPrueba.CAPSULA_LA_MONEDA.id, punto(DatosPrueba.PUNTO_LA_MONEDA.id).capsulaId)
    }

    @Test
    fun alRetirarDeUnPuntoInactivoElPuntoQuedaSinCapsula() = runBlocking {
        //Parque Almagro está inactivo con su cápsula publicada
        assertEquals(ResultadoCapsula.Listo, gestor.retirar(DatosPrueba.CAPSULA_PARQUE_ALMAGRO.id))

        val almagro = punto(DatosPrueba.PUNTO_PARQUE_ALMAGRO.id)
        assertNull(almagro.capsulaId)
        assertEquals(EstadoAdminPunto.SIN_CAPSULA, EstadoAdminPunto.desde(almagro, null))
        //la cápsula vuelve a borrador
        val retirada = capsula(DatosPrueba.CAPSULA_PARQUE_ALMAGRO.id)!!
        assertEquals(EstadoCapsula.BORRADOR, retirada.estado)
        assertNull(retirada.publicadaEn)
    }

    @Test
    fun seRetiraUnaCapsulaSinPunto() = runBlocking {
        assertEquals(ResultadoCapsula.Listo, gestor.retirar(DatosPrueba.CAPSULA_LIBRE.id))
        assertEquals(EstadoCapsula.BORRADOR, capsula(DatosPrueba.CAPSULA_LIBRE.id)!!.estado)
    }

    @Test
    fun soloSeEliminanBorradores() = runBlocking {
        assertEquals(ResultadoCapsula.Error("Solo se pueden eliminar los borradores"), gestor.eliminar(DatosPrueba.CAPSULA_LA_MONEDA.id))
        assertEquals(ResultadoCapsula.Listo, gestor.eliminar(DatosPrueba.CAPSULA_BORRADOR.id))
        assertNull(capsula(DatosPrueba.CAPSULA_BORRADOR.id))
    }

    @Test
    fun elDetalleMuestraElPuntoYLasVistasCompletas() {
        val progreso = FakeProgresoRepository(DatosPrueba.progreso)
        val detalle = CapsulaDetalleViewModel(DatosPrueba.CAPSULA_LA_MONEDA.id, capsulas, puntos, progreso, gestor)

        val estado = detalle.estado.value
        assertEquals(DatosPrueba.PUNTO_LA_MONEDA.id, estado.punto?.id)
        assertEquals(EstadoAdminPunto.ACTIVO, estado.estadoPunto)
        //filas de La Moneda que ya sumaron los 50 de la cápsula
        val esperadas = DatosPrueba.progreso.count { it.puntoId == DatosPrueba.PUNTO_LA_MONEDA.id && it.puntosCapsula == 50 }
        assertTrue(esperadas > 0)
        assertEquals(esperadas, estado.vistasCompletas)
        //con el punto activo el botón no abre el diálogo
        assertEquals(ReglaCapsula.RETIRO_BLOQUEADO, estado.motivoNoRetirable)
        detalle.onEvent(CapsulaDetalleUiEvent.OnRetirar)
        assertEquals(false, detalle.estado.value.confirmarRetiro)
    }

    @Test
    fun elDetallePideConfirmacionYRetira() {
        val detalle = CapsulaDetalleViewModel(DatosPrueba.CAPSULA_PARQUE_ALMAGRO.id, capsulas, puntos, FakeProgresoRepository(), gestor)

        detalle.onEvent(CapsulaDetalleUiEvent.OnRetirar)
        assertTrue(detalle.estado.value.confirmarRetiro)
        detalle.onEvent(CapsulaDetalleUiEvent.OnConfirmarRetiro)

        assertTrue(detalle.estado.value.retirada)
        assertNull(punto(DatosPrueba.PUNTO_PARQUE_ALMAGRO.id).capsulaId)
    }
}
