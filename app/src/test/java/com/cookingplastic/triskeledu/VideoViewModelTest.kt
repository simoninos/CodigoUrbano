package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.red.FakeRedRepository
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.ui.event.VideoUiEvent
import com.cookingplastic.triskeledu.ui.state.ModoReproductor
import com.cookingplastic.triskeledu.ui.validation.EnlaceVideo
import com.cookingplastic.triskeledu.ui.viewmodels.VideoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

//RN-08, RN-35, RN-36 y ACT-02 con los Fake: Camila tiene La Moneda sin ver (video V1 en Drive),
//Catedral sin ver (sin enlace) y Bellas Artes ya vista. Parte con 160 puntos
@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModelTest {

    private val progreso = FakeProgresoRepository(DatosPrueba.progreso)
    private val usuarios = FakeUsuarioRepository()
    private val red = FakeRedRepository(conectado = true)
    private val drive = "https://drive.google.com/file/d/1AbC-dEf_123/view?usp=sharing"

    @Before
    fun antes() {
        //UnconfinedTestDispatcher: lo que el ViewModel lanza en viewModelScope corre al tiro
        Dispatchers.setMain(UnconfinedTestDispatcher())
        runBlocking { usuarios.iniciarSesion("camila.explora@ejemplo.cl", "Demo1234") }
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun abrir(
        capsula: Capsula,
        punto: PuntoInteres?,
        vistaPrevia: Boolean = false,
        capsulas: List<Capsula> = DatosPrueba.capsulas
    ) = VideoViewModel(
        capsulaId = capsula.id,
        puntoId = punto?.id,
        esVistaPrevia = vistaPrevia,
        capsulaRepository = FakeCapsulaRepository(capsulas),
        puntoInteresRepository = FakePuntoInteresRepository(),
        usuarioRepository = usuarios,
        progresoRepository = progreso,
        redRepository = red
    )

    private fun puntosDeCamila() = runBlocking { progreso.totalPuntos(DatosPrueba.USUARIO_CAMILA.id).first() }

    //las cápsulas de prueba con un enlace de Drive puesto, para probar los videos de internet
    private fun conDrive(capsula: Capsula) = DatosPrueba.capsulas.map { if (it.id == capsula.id) it.copy(urlStream = drive) else it }

    //RN-36: si se quita el enlace de La Moneda, vuelve el clip incluido en la app
    private fun conClip() = DatosPrueba.capsulas.map {
        if (it.id == DatosPrueba.CAPSULA_LA_MONEDA.id) it.copy(urlStream = DatosPrueba.CLIP_PRUEBA) else it
    }

    @Test
    fun sinVerLaCapsulaVaBloqueado() {
        val estado = abrir(DatosPrueba.CAPSULA_LA_MONEDA, DatosPrueba.PUNTO_LA_MONEDA).estado.value

        assertEquals(ModoReproductor.BLOQUEADO, estado.modo)
        assertFalse(estado.puedeAdelantar)
        //V1: el enlace de compartir de Drive se convierte en descarga directa
        assertEquals("https://drive.google.com/uc?export=download&id=1bli39LeGysXEh2Ax6gyAkHrQa0e8VVAY", estado.enlace)
        assertEquals("Palacio de La Moneda", estado.nombrePunto)
    }

    @Test
    fun yaVistaVaLibre() {
        val capsula = DatosPrueba.CAPSULA_BELLAS_ARTES
        val estado = abrir(capsula, DatosPrueba.PUNTO_BELLAS_ARTES, capsulas = conDrive(capsula)).estado.value

        assertEquals(ModoReproductor.LIBRE, estado.modo)
        assertTrue(estado.puedeAdelantar)
    }

    @Test
    fun alTerminarSuma50UnaSolaVez() {
        val video = abrir(DatosPrueba.CAPSULA_LA_MONEDA, DatosPrueba.PUNTO_LA_MONEDA)
        video.onEvent(VideoUiEvent.OnTerminada)
        video.onEvent(VideoUiEvent.OnTerminada)

        assertEquals(50, video.estado.value.puntosGanados)
        assertEquals(210, puntosDeCamila())
        //desde ahora ya está vista: pasa a libre
        assertEquals(ModoReproductor.LIBRE, video.estado.value.modo)

        //verla de nuevo (ACT-06) no suma
        val otraVez = abrir(DatosPrueba.CAPSULA_LA_MONEDA, DatosPrueba.PUNTO_LA_MONEDA)
        assertEquals(ModoReproductor.LIBRE, otraVez.estado.value.modo)
        otraVez.onEvent(VideoUiEvent.OnTerminada)
        assertEquals(0, otraVez.estado.value.puntosGanados)
        assertEquals(210, puntosDeCamila())
    }

    @Test
    fun laVistaPreviaEsLibreYNoSuma() {
        val video = abrir(DatosPrueba.CAPSULA_LA_MONEDA, DatosPrueba.PUNTO_LA_MONEDA, vistaPrevia = true)
        assertEquals(ModoReproductor.LIBRE, video.estado.value.modo)

        video.onEvent(VideoUiEvent.OnTerminada)
        assertEquals(0, video.estado.value.puntosGanados)
        assertEquals(160, puntosDeCamila())
    }

    @Test
    fun sinEnlaceSimulaLaVistaYSuma50UnaVez() {
        //RN-35: la Catedral no tiene video
        val primera = abrir(DatosPrueba.CAPSULA_CATEDRAL, DatosPrueba.PUNTO_CATEDRAL).estado.value
        assertTrue(primera.sinEnlace)
        assertNull(primera.enlace)
        assertEquals(50, primera.puntosGanados)
        assertEquals(210, puntosDeCamila())

        val segunda = abrir(DatosPrueba.CAPSULA_CATEDRAL, DatosPrueba.PUNTO_CATEDRAL).estado.value
        assertTrue(segunda.yaVista)
        assertEquals(0, segunda.puntosGanados)
        assertEquals(210, puntosDeCamila())
    }

    @Test
    fun sinEscanearElLugarNoSeVe() {
        //RN-07: Camila no ha escaneado el Mercado Central
        val estado = abrir(DatosPrueba.CAPSULA_MERCADO_CENTRAL, DatosPrueba.PUNTO_MERCADO_CENTRAL).estado.value

        assertEquals("Escanea el QR del lugar para ver su cápsula", estado.error)
        assertEquals(160, puntosDeCamila())
    }

    @Test
    fun elClipDePruebaNoNecesitaInternet() {
        red.conectado = false
        val estado = abrir(DatosPrueba.CAPSULA_LA_MONEDA, DatosPrueba.PUNTO_LA_MONEDA, capsulas = conClip()).estado.value

        assertFalse(estado.sinConexion)
    }

    @Test
    fun videoDeDriveSinInternetPideReintentar() {
        val capsula = DatosPrueba.CAPSULA_LA_MONEDA
        red.conectado = false
        val video = abrir(capsula, DatosPrueba.PUNTO_LA_MONEDA, capsulas = conDrive(capsula))
        assertTrue(video.estado.value.sinConexion)

        //sigue sin red: el aviso se queda
        video.onEvent(VideoUiEvent.OnReintentar)
        assertTrue(video.estado.value.sinConexion)

        //vuelve la red: se reintenta desde donde iba
        red.conectado = true
        video.onEvent(VideoUiEvent.OnReintentar)
        assertFalse(video.estado.value.sinConexion)
        assertEquals(1, video.estado.value.intento)
    }

    @Test
    fun siSeCortaLaRedSePuedeReintentarYSiFallaElArchivoNo() {
        //EXC-05 y EXC-11
        val cortada = abrir(DatosPrueba.CAPSULA_LA_MONEDA, DatosPrueba.PUNTO_LA_MONEDA)
        cortada.onEvent(VideoUiEvent.OnAvance(posicionMs = 8_000, duracionMs = 20_000))
        cortada.onEvent(VideoUiEvent.OnErrorReproduccion(esDeRed = true))
        assertTrue(cortada.estado.value.sinConexion)
        assertEquals(8_000, cortada.estado.value.posicionMs)

        val rota = abrir(DatosPrueba.CAPSULA_LA_MONEDA, DatosPrueba.PUNTO_LA_MONEDA)
        rota.onEvent(VideoUiEvent.OnErrorReproduccion(esDeRed = false))
        assertEquals("Esta cápsula no está disponible por ahora", rota.estado.value.error)
        //no sumó nada en ninguno de los dos casos
        assertEquals(160, puntosDeCamila())
    }

    @Test
    fun enlaceDeDriveADescargaDirecta() {
        assertEquals("1AbC-dEf_123", EnlaceVideo.idDeDrive(drive))
        assertEquals("1AbC-dEf_123", EnlaceVideo.idDeDrive("https://drive.google.com/open?id=1AbC-dEf_123"))
        assertEquals(
            "https://drive.google.com/uc?export=download&id=1AbC-dEf_123",
            EnlaceVideo.aReproducible(drive)
        )
        //el clip incluido queda igual y no necesita internet
        assertEquals(DatosPrueba.CLIP_PRUEBA, EnlaceVideo.aReproducible(DatosPrueba.CLIP_PRUEBA))
        assertFalse(EnlaceVideo.necesitaInternet(DatosPrueba.CLIP_PRUEBA))
        assertTrue(EnlaceVideo.necesitaInternet(drive))
        assertNull(EnlaceVideo.idDeDrive("https://example.com/video.mp4"))
    }
}
