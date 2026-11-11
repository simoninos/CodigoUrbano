package com.cookingplastic.triskeledu.ui.screens.mislugares

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.location.FakeUbicacionRepository
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakePreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.navigation.PedidoMapa
import com.cookingplastic.triskeledu.ui.screens.mapa.MapaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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

//LUG-01 y LUG-03: los contadores se actualizan solos y "Por descubrir" abre el mapa en el lugar
@OptIn(ExperimentalCoroutinesApi::class)
class MisDescubrimientosViewModelTest {

    private val camila = DatosPrueba.USUARIO_CAMILA
    private val ovalle = DatosPrueba.PUNTO_PADRE_ALONSO_OVALLE
    private lateinit var progreso: FakeProgresoRepository
    private lateinit var usuarios: FakeUsuarioRepository
    private lateinit var pedido: PedidoMapa
    private lateinit var misLugares: MisLugaresViewModel

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        progreso = FakeProgresoRepository(DatosPrueba.progreso)
        usuarios = FakeUsuarioRepository()
        pedido = PedidoMapa()
        runBlocking { usuarios.iniciarSesion(camila.correo, DatosPrueba.CLAVE_DE_PRUEBA) }
        misLugares = MisLugaresViewModel(
            progresoRepository = progreso,
            usuarioRepository = usuarios,
            puntosActivos = FakePuntoInteresRepository().obtenerActivos(),
            capsulas = FakeCapsulaRepository().capsulas(),
            pedidoMapa = pedido
        )
    }

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun contadoresDeCamila() {
        val estado = misLugares.estado.value

        //14 lugares activos; Camila escaneó 4, vio 2 cápsulas y tomó 1 selfie
        assertEquals(14, estado.totalLugares)
        assertEquals(4, estado.qrEscaneados)
        assertEquals(2, estado.capsulasVistas)
        assertEquals(1, estado.selfiesTomadas)
        assertEquals(160, estado.totalPuntos)
    }

    @Test
    fun losContadoresCambianAlEscanearYVerCapsula() = runBlocking {
        progreso.registrarEscaneo(camila.id, ovalle.id, DatosPrueba.UBICACION_CERCA_LA_MONEDA)
        progreso.registrarVistaCapsula(camila.id, DatosPrueba.PUNTO_LA_MONEDA.id)

        val estado = misLugares.estado.value
        assertEquals(5, estado.qrEscaneados)
        assertEquals(3, estado.capsulasVistas)
        assertEquals(160 + 10 + 50, estado.totalPuntos)
        //el lugar recién escaneado ya no está por descubrir
        assertFalse(estado.porDescubrir.any { it.puntoId == ovalle.id })
    }

    @Test
    fun cambiaDeVista() {
        misLugares.onEvent(MisLugaresUiEvent.OnVistaChange(VistaDescubrimientos.SELFIES))

        val estado = misLugares.estado.value
        assertEquals(VistaDescubrimientos.SELFIES, estado.vista)
        assertEquals(1, estado.selfies.size)
        //13 lugares sin selfie: 10 sin escanear y 3 escaneados
        assertEquals(13, estado.porDescubrir.size)
    }

    @Test
    fun verEnMapaDejaElPedidoYElMapaLoAtiende() {
        misLugares.onEvent(MisLugaresUiEvent.OnVerEnMapa(ovalle.id))
        assertEquals(ovalle.id, pedido.puntoId.value)
        assertTrue(misLugares.estado.value.irAlMapa)

        val mapa = nuevoMapa()

        val estado = mapa.estado.value
        assertEquals(ovalle.id, estado.seleccionadoId)
        assertEquals(ovalle.ubicacion.latitud, estado.camara!!.latitud, 0.0)
        assertEquals(ovalle.ubicacion.longitud, estado.camara!!.longitud, 0.0)
        assertEquals(17.0, estado.camara!!.zoom, 0.0)
        //el pedido se atiende una sola vez
        assertNull(pedido.puntoId.value)

        misLugares.onEvent(MisLugaresUiEvent.OnMapaAbierto)
        assertFalse(misLugares.estado.value.irAlMapa)
    }

    @Test
    fun elMapaAbiertoTambienRecibeElPedido() {
        //el mapa ya existe (es la primera pestaña): el pedido llega después
        val mapa = nuevoMapa()
        misLugares.onEvent(MisLugaresUiEvent.OnVerEnMapa(DatosPrueba.PUNTO_CATEDRAL.id))

        assertEquals(DatosPrueba.PUNTO_CATEDRAL.id, mapa.estado.value.seleccionadoId)
        assertNull(pedido.puntoId.value)
    }

    private fun nuevoMapa() = MapaViewModel(
        FakePuntoInteresRepository(),
        FakeUbicacionRepository(),
        usuarios,
        FakePreferenciasRepository(),
        progreso,
        pedido,
        FakeCapsulaRepository()
    )
}
