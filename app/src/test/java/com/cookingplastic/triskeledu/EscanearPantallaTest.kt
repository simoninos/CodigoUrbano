package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.data.creador.GestorCapsulas
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.location.FakeUbicacionRepository
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.ui.event.CapsulaPreviaUiEvent
import com.cookingplastic.triskeledu.ui.event.EscanearUiEvent
import com.cookingplastic.triskeledu.ui.state.EstadoGps
import com.cookingplastic.triskeledu.ui.viewmodels.CapsulaPreviaViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.EscanearViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.ProcesadorEscaneo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

//P05 Escanear (ESC-01, RN-04, RN-05, EXC-01, EXC-04) y P18-B Vista previa (CRE-03): los ViewModels completos,
//con el procesador real y las Fakes. Las reglas del procesador solo están en EscanearViewModelTest
@OptIn(ExperimentalCoroutinesApi::class)
class EscanearPantallaTest {

    private val moneda = DatosPrueba.PUNTO_LA_MONEDA
    private lateinit var ubicacion: FakeUbicacionRepository
    private lateinit var progreso: FakeProgresoRepository
    private lateinit var usuarios: FakeUsuarioRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        ubicacion = FakeUbicacionRepository()
        progreso = FakeProgresoRepository()
        usuarios = FakeUsuarioRepository()
    }

    @After
    fun despues() = Dispatchers.resetMain()

    private fun viewModel(progresoRepo: ProgresoRepository = progreso) = EscanearViewModel(
        ProcesadorEscaneo(FakePuntoInteresRepository(), ubicacion, progresoRepo), ubicacion, usuarios
    )

    private suspend fun conSesionYGps(vm: EscanearViewModel) {
        usuarios.iniciarSesion(DatosPrueba.USUARIO_NUEVA.correo, DatosPrueba.CLAVE_DE_PRUEBA)
        vm.onEvent(EscanearUiEvent.OnPermisoUbicacion(true))
        vm.onEvent(EscanearUiEvent.OnGpsEncendido(true))
    }

    @Test
    fun sinPermisoDeUbicacion_noSeEscanea_EXC01() = runTest {
        val vm = viewModel()
        usuarios.iniciarSesion(DatosPrueba.USUARIO_NUEVA.correo, DatosPrueba.CLAVE_DE_PRUEBA)

        vm.onEvent(EscanearUiEvent.OnCodigoLeido(moneda.codigoQr))

        assertEquals(EstadoGps.SIN_PERMISO, vm.estado.value.gps)
        assertNull(vm.estado.value.codigoLeido)
    }

    @Test
    fun gpsApagado_muestraElEstado_yNoEscanea() = runTest {
        val vm = viewModel()
        vm.onEvent(EscanearUiEvent.OnPermisoUbicacion(true))
        vm.onEvent(EscanearUiEvent.OnGpsEncendido(false))

        vm.onEvent(EscanearUiEvent.OnCodigoLeido(moneda.codigoQr))

        assertEquals(EstadoGps.APAGADO, vm.estado.value.gps)
        assertNull(vm.estado.value.codigoLeido)
    }

    @Test
    fun aMenosDe100m_desbloqueaElLugar_ySuma10Puntos() = runTest {
        val vm = viewModel()
        conSesionYGps(vm)
        assertEquals(EstadoGps.ACTIVO, vm.estado.value.gps)

        vm.onEvent(EscanearUiEvent.OnCodigoLeido("  ${moneda.codigoQr}  "))

        val encontrado = vm.estado.value.encontrado
        assertEquals(moneda.id, encontrado?.puntoId)
        assertTrue(encontrado!!.distanciaMetros <= 100)
        assertNotNull(progreso.progresoDePunto(DatosPrueba.USUARIO_NUEVA.id, moneda.id).first())
    }

    @Test
    fun lejos_seRechaza_yEscanearOtroLimpiaLaPantalla_RN04() = runTest {
        ubicacion.moverA(Ubicacion(latitud = -33.4500, longitud = -70.6505, altitud = 0.0))
        val vm = viewModel()
        conSesionYGps(vm)

        vm.onEvent(EscanearUiEvent.OnCodigoLeido(moneda.codigoQr))
        assertNotNull(vm.estado.value.mensajeRechazo)
        //la cámara ve el mismo QR muchas veces: mientras hay un resultado no se procesa otro
        vm.onEvent(EscanearUiEvent.OnCodigoLeido("OTRO"))
        assertEquals(moneda.codigoQr, vm.estado.value.codigoLeido)

        vm.onEvent(EscanearUiEvent.OnEscanearOtro)
        assertNull(vm.estado.value.codigoLeido)
        assertNull(vm.estado.value.mensajeRechazo)
    }

    @Test
    fun unLugarQueYaTenia_seAbreSinSumar_EXC04() = runTest {
        val vm = viewModel()
        conSesionYGps(vm)
        vm.onEvent(EscanearUiEvent.OnCodigoLeido(moneda.codigoQr))
        vm.onEvent(EscanearUiEvent.OnEscanearOtro)

        vm.onEvent(EscanearUiEvent.OnCodigoLeido(moneda.codigoQr))

        assertEquals(moneda.id, vm.estado.value.yaDescubierto?.puntoId)
        assertNull(vm.estado.value.encontrado)
    }

    @Test
    fun unQrQueNoEsDelJuego_yElPermisoDeCamara() = runTest {
        val vm = viewModel()
        conSesionYGps(vm)
        vm.onEvent(EscanearUiEvent.OnPermisoCamara(false))

        vm.onEvent(EscanearUiEvent.OnCodigoLeido("https://otra-cosa.cl"))

        assertEquals(ProcesadorEscaneo.CODIGO_AJENO, vm.estado.value.mensajeRechazo)
        assertEquals(false, vm.estado.value.permisoCamara)
    }

    @Test
    fun siFallaLaBase_seMuestraUnMensaje_yLaAppSigue() = runTest {
        val roto = mockk<ProgresoRepository>()
        coEvery { roto.progresoDePunto(any(), any()) } returns flow { error("base dañada") }
        val vm = viewModel(roto)
        conSesionYGps(vm)

        vm.onEvent(EscanearUiEvent.OnCodigoLeido(moneda.codigoQr))

        assertEquals("No se pudo registrar el escaneo, intenta de nuevo", vm.estado.value.mensajeRechazo)
        assertEquals(false, vm.estado.value.procesando)
    }

    @Test
    fun sinSenalDeGps_seMuestraElEstado() = runTest {
        val sinSenal = object : com.cookingplastic.triskeledu.data.location.UbicacionRepository {
            override fun ubicaciones() = flow<Ubicacion> { throw IllegalStateException("sin señal") }
            override suspend fun ultimaUbicacion(): Ubicacion? = null
        }
        val vm = EscanearViewModel(ProcesadorEscaneo(FakePuntoInteresRepository(), sinSenal, progreso), sinSenal, usuarios)

        vm.onEvent(EscanearUiEvent.OnPermisoUbicacion(true))

        assertEquals(EstadoGps.SIN_SENAL, vm.estado.value.gps)
    }

    // ---- P18-B Vista previa (CRE-03 y CRE-05) ----

    private val borrador = DatosPrueba.capsulas.first().copy(id = "cap-previa", estado = EstadoCapsula.BORRADOR)

    private fun previa(capsulas: FakeCapsulaRepository) =
        CapsulaPreviaViewModel("cap-previa", capsulas, GestorCapsulas(capsulas, FakePuntoInteresRepository(capsulaRepository = capsulas)))

    @Test
    fun previa_sinQueCargueElVideo_noPublica() = runTest {
        val vm = previa(FakeCapsulaRepository(listOf(borrador)))
        assertEquals(EstadoCapsula.BORRADOR, vm.estado.value.estadoCapsula)

        vm.onEvent(CapsulaPreviaUiEvent.OnPublicar(videoCargado = false, duracionMs = 0))

        assertEquals("Espera a que el video cargue para confirmar que el enlace funciona", vm.estado.value.mensajeError)
    }

    @Test
    fun previa_unVideoDeMasDe3Minutos_noPublica_RN26() = runTest {
        val vm = previa(FakeCapsulaRepository(listOf(borrador)))

        vm.onEvent(CapsulaPreviaUiEvent.OnPublicar(videoCargado = true, duracionMs = 200_000))

        assertEquals("El video dura más de 3 minutos: acórtalo antes de publicarlo", vm.estado.value.mensajeError)
    }

    @Test
    fun previa_conElVideoListo_publica() = runTest {
        val capsulas = FakeCapsulaRepository(listOf(borrador))
        val vm = previa(capsulas)

        vm.onEvent(CapsulaPreviaUiEvent.OnPublicar(videoCargado = true, duracionMs = 90_000))

        assertTrue(vm.estado.value.publicada)
        assertEquals(EstadoCapsula.PUBLICADA, capsulas.obtenerPorId("cap-previa")?.estado)
    }

    @Test
    fun previa_siLaCapsulaNoExiste_muestraElError() = runTest {
        val vm = previa(FakeCapsulaRepository(emptyList()))

        vm.onEvent(CapsulaPreviaUiEvent.OnPublicar(videoCargado = true, duracionMs = 90_000))

        assertEquals("Esta cápsula ya no existe", vm.estado.value.mensajeError)
    }
}
