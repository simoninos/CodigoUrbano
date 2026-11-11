package com.cookingplastic.triskeledu.ui.shared

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
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

//ACC-04, ACC-08, RN-18 y RN-29 en el login
@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {

    private lateinit var sesion: SesionViewModel

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        sesion = SesionViewModel(FakeUsuarioRepository(), FakeProgresoRepository(DatosPrueba.progreso))
    }

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun sinDatosMuestraLosDosErrores() {
        sesion.onEvent(SesionUiEvent.OnIniciarSesion)

        assertEquals("Ingresa un correo válido", sesion.estado.value.errorCorreo)
        assertEquals("Ingresa tu contraseña", sesion.estado.value.errorClave)
        assertEquals(CampoSesion.CORREO, sesion.estado.value.campoConError)
        assertFalse(sesion.estado.value.sesionIniciada)
    }

    @Test
    fun usuariosDeLaDemoConSuNivel() {
        val porNombre = sesion.estado.value.usuariosDemo.associateBy { it.username }

        assertEquals(10, porNombre.size)
        assertEquals("usuaria_nueva · Nvl 1", porNombre.getValue("usuaria_nueva").etiqueta)
        assertEquals("camila_explora · Nvl 3", porNombre.getValue("camila_explora").etiqueta)
        assertEquals("contenido_ia · Nvl 5", porNombre.getValue("contenido_ia").etiqueta)
        assertEquals("maestro_demo · Nvl 8", porNombre.getValue("maestro_demo").etiqueta)
        assertEquals("pendiente_demo · En revisión", porNombre.getValue("pendiente_demo").etiqueta)
        //el administrador no tiene nivel
        assertNull(porNombre.getValue("admin_triskel").nivel)
        assertEquals(RolUsuario.ADMINISTRADOR, porNombre.getValue("admin_triskel").rol)
    }

    @Test
    fun accesoRapidoSoloRellena() {
        sesion.onEvent(SesionUiEvent.OnAccesoRapido("maestro.demo@ejemplo.cl"))

        assertEquals("maestro.demo@ejemplo.cl", sesion.estado.value.correo)
        assertEquals("Demo1234", sesion.estado.value.clave)
        //no inicia sesión hasta tocar el botón
        assertFalse(sesion.estado.value.sesionIniciada)

        sesion.onEvent(SesionUiEvent.OnIniciarSesion)
        assertTrue(sesion.estado.value.sesionIniciada)
    }

    @Test
    fun cuentaEnRevisionNoEntraYVaAP23() {
        sesion.onEvent(SesionUiEvent.OnAccesoRapido("pendiente.demo@ejemplo.cl"))
        sesion.onEvent(SesionUiEvent.OnIniciarSesion)

        assertTrue(sesion.estado.value.solicitudPendiente)
        assertFalse(sesion.estado.value.sesionIniciada)
        assertNull(sesion.estado.value.mensajeError)
    }

    @Test
    fun claveIncorrecta() {
        sesion.onEvent(SesionUiEvent.OnCorreoChange("camila.explora@ejemplo.cl"))
        sesion.onEvent(SesionUiEvent.OnClaveChange("Otra1234"))
        sesion.onEvent(SesionUiEvent.OnIniciarSesion)

        assertEquals("Correo o contraseña incorrectos", sesion.estado.value.mensajeError)
        assertFalse(sesion.estado.value.solicitudPendiente)
    }
}
