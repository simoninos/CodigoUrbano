package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.ui.event.ServidorUiEvent
import com.cookingplastic.triskeledu.ui.viewmodels.ServidorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

//BKD-01: la dirección del gateway se escribe en la app (demo con el celular como punto de acceso)
@OptIn(ExperimentalCoroutinesApi::class)
class DireccionServidorTest {

    private var guardada: String? = null
    private fun direccion() = DireccionServidor("http://10.0.2.2:8080/", leer = { guardada }, guardar = { guardada = it })

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun normalizar_aceptaLoQueSeEscribeEnLaDemo() {
        assertEquals("http://192.168.43.10:8080/", DireccionServidor.normalizar("192.168.43.10").toString())
        assertEquals("http://192.168.43.10:9000/", DireccionServidor.normalizar(" 192.168.43.10:9000 ").toString())
        assertEquals("http://192.168.43.10:8080/", DireccionServidor.normalizar("http://192.168.43.10:8080/api/puntos?x=1").toString())
        assertEquals("https://gateway.ejemplo.cl:8080/", DireccionServidor.normalizar("https://gateway.ejemplo.cl").toString())
        assertNull(DireccionServidor.normalizar(""))
        assertNull(DireccionServidor.normalizar("192.168 .43.10"))
        assertNull(DireccionServidor.normalizar("ftp://192.168.43.10"))
    }

    @Test
    fun sinNadaGuardado_usaLaDeFabrica_yLaQueSeGuardaSobreviveAlReiniciar() {
        val primera = direccion()
        assertEquals("http://10.0.2.2:8080/", primera.actual.value.toString())

        primera.cambiar("http://192.168.43.10:8080/".toHttpUrl())

        //otra instancia = la app abierta de nuevo
        assertEquals("http://192.168.43.10:8080/", direccion().actual.value.toString())
        direccion().restablecer()
        assertNull(guardada)
    }

    @Test
    fun cadaPeticionVaALaDireccionGuardadaEnEseMomento() = runTest {
        val servidor = MockWebServer().apply { start() }
        servidor.enqueue(MockResponse().setBody("[]"))
        servidor.enqueue(MockResponse().setBody("[]"))
        val dir = direccion()
        //Retrofit se crea con la dirección de fábrica, que aquí no existe
        val api = ClienteBackend.crear("http://10.0.2.2:8080/", token = { null }, alVencer = {}, servidor = { dir.actual.value })
        try {
            dir.cambiar(servidor.url("/"))
            api.puntos()
            api.capsulas()

            assertEquals("/api/puntos", servidor.takeRequest().path)
            assertEquals("/api/capsulas", servidor.takeRequest().path)
        } finally {
            servidor.shutdown()
        }
    }

    @Test
    fun responde_soloSiElGatewayContesta() = runTest {
        val servidor = MockWebServer().apply { start() }
        servidor.enqueue(MockResponse().setBody("""{"status":"UP"}"""))
        val url = servidor.url("/")

        assertTrue(DireccionServidor.responde(url))
        assertEquals("/actuator/health", servidor.takeRequest().path)
        servidor.shutdown()
        assertFalse(DireccionServidor.responde(url))
    }

    @Test
    fun viewModel_validaGuardaYPrueba() = runTest {
        val dir = direccion()
        var probada: String? = null
        val vm = ServidorViewModel(dir, probar = { probada = it.toString(); true })
        assertEquals("http://10.0.2.2:8080", vm.estado.value.actual)

        vm.onEvent(ServidorUiEvent.OnAbrir)
        vm.onEvent(ServidorUiEvent.OnTextoChange("no es una ip"))
        vm.onEvent(ServidorUiEvent.OnGuardar)
        assertNotNull(vm.estado.value.error)
        assertNull(guardada)

        vm.onEvent(ServidorUiEvent.OnTextoChange("192.168.43.10"))
        vm.onEvent(ServidorUiEvent.OnGuardar)
        assertEquals("http://192.168.43.10:8080/", probada)
        assertEquals(true, vm.estado.value.responde)
        assertEquals("http://192.168.43.10:8080", vm.estado.value.actual)

        vm.onEvent(ServidorUiEvent.OnRestablecer)
        vm.onEvent(ServidorUiEvent.OnCerrar)
        assertEquals("http://10.0.2.2:8080", vm.estado.value.actual)
        assertFalse(vm.estado.value.dialogoAbierto)
    }

    @Test
    fun viewModel_siNoResponde_igualQuedaGuardada() = runTest {
        val dir = direccion()
        val vm = ServidorViewModel(dir, probar = { false })

        vm.onEvent(ServidorUiEvent.OnTextoChange("192.168.43.10:8080"))
        vm.onEvent(ServidorUiEvent.OnGuardar)

        assertEquals(false, vm.estado.value.responde)
        assertEquals("http://192.168.43.10:8080/", guardada)
    }
}
