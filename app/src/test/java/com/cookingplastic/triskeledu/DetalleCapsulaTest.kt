package com.cookingplastic.triskeledu

import androidx.lifecycle.SavedStateHandle
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.location.FakeUbicacionRepository
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.red.FakeRedRepository
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakeComentarioRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.ui.event.DetalleUiEvent
import com.cookingplastic.triskeledu.ui.viewmodels.DetalleViewModel
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

//P04 → P12: el botón de la cápsula abre el reproductor solo con el punto desbloqueado (RN-07),
//y pide internet solo si el video es de Drive (ACT-02)
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleCapsulaTest {

    private val usuarios = FakeUsuarioRepository()
    private val red = FakeRedRepository(conectado = true)

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        runBlocking { usuarios.iniciarSesion("camila.explora@ejemplo.cl", "Demo1234") }
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun detalle(punto: PuntoInteres, capsulas: List<Capsula> = DatosPrueba.capsulas) = DetalleViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Rutas.ARG_PUNTO_ID to punto.id)),
        puntoInteresRepository = FakePuntoInteresRepository(),
        ubicacionRepository = FakeUbicacionRepository(),
        redRepository = red,
        usuarioRepository = usuarios,
        progresoRepository = FakeProgresoRepository(DatosPrueba.progreso),
        comentarioRepository = FakeComentarioRepository(),
        capsulaRepository = FakeCapsulaRepository(capsulas)
    )

    @Test
    fun desbloqueadoAbreElReproductor() {
        val vm = detalle(DatosPrueba.PUNTO_LA_MONEDA)
        vm.onEvent(DetalleUiEvent.OnVerVideo)
        assertEquals(DatosPrueba.CAPSULA_LA_MONEDA.id, vm.estado.value.capsulaPorAbrir)

        //la pantalla navega y avisa: no se vuelve a abrir sola
        vm.onEvent(DetalleUiEvent.OnCapsulaAbierta)
        assertNull(vm.estado.value.capsulaPorAbrir)
    }

    @Test
    fun bloqueadoNoAbre() {
        //Camila no ha escaneado el Mercado Central
        val vm = detalle(DatosPrueba.PUNTO_MERCADO_CENTRAL)
        vm.onEvent(DetalleUiEvent.OnVerVideo)

        assertNull(vm.estado.value.capsulaPorAbrir)
        assertEquals("Escanea el QR del lugar para desbloquear esta actividad", vm.estado.value.mensaje)
    }

    @Test
    fun soloDrivePideInternet() {
        red.conectado = false
        //el clip de prueba se abre igual (RN-36: es el respaldo si se quita el enlace de La Moneda)
        val conClip = DatosPrueba.capsulas.map {
            if (it.id == DatosPrueba.CAPSULA_LA_MONEDA.id) it.copy(urlStream = DatosPrueba.CLIP_PRUEBA) else it
        }
        val clip = detalle(DatosPrueba.PUNTO_LA_MONEDA, conClip)
        clip.onEvent(DetalleUiEvent.OnVerVideo)
        assertFalse(clip.estado.value.mostrarSinConexion)
        assertEquals(DatosPrueba.CAPSULA_LA_MONEDA.id, clip.estado.value.capsulaPorAbrir)

        //con su video de Drive (V1) aparece el aviso sin conexión (P11)
        val drive = detalle(DatosPrueba.PUNTO_LA_MONEDA)
        drive.onEvent(DetalleUiEvent.OnVerVideo)
        assertTrue(drive.estado.value.mostrarSinConexion)
        assertNull(drive.estado.value.capsulaPorAbrir)
    }
}
