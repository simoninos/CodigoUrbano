package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.event.RegistroUiEvent
import com.cookingplastic.triskeledu.ui.state.CampoRegistro
import com.cookingplastic.triskeledu.ui.viewmodels.RegistroViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

//ACC-02, ACC-03 (RN-18) y ACC-07
@OptIn(ExperimentalCoroutinesApi::class)
class RegistroViewModelTest {

    //cuenta cuántas veces se intentó crear una cuenta; lo demás lo hace el Fake
    private class ContadorRegistros(private val real: UsuarioRepository) : UsuarioRepository by real {
        var registros = 0
        override suspend fun registrar(username: String, correo: String, clave: String, version: String, quiereSerCreador: Boolean) =
            real.registrar(username, correo, clave, version, quiereSerCreador).also { registros++ }
    }

    private val usuarios = ContadorRegistros(FakeUsuarioRepository())
    private val registro = RegistroViewModel(usuarios)

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    private fun llenar(terminos: Boolean = true, creador: Boolean = false) {
        registro.onEvent(RegistroUiEvent.OnUsernameChange("nueva_cuenta"))
        registro.onEvent(RegistroUiEvent.OnCorreoChange("Nueva.Cuenta@Ejemplo.cl"))
        registro.onEvent(RegistroUiEvent.OnClaveChange("Clave1234"))
        registro.onEvent(RegistroUiEvent.OnRepetirClaveChange("Clave1234"))
        if (terminos) registro.onEvent(RegistroUiEvent.OnTerminosChange(true))
        if (creador) registro.onEvent(RegistroUiEvent.OnQuiereSerCreadorChange(true))
    }

    @Test
    fun formularioVacioMuestraTodosLosErroresYNoCreaLaCuenta() {
        registro.onEvent(RegistroUiEvent.OnRegistrar)
        val estado = registro.estado.value

        assertEquals("El nombre de usuario es obligatorio", estado.errorUsername)
        assertEquals("Ingresa un correo válido", estado.errorCorreo)
        assertEquals("Mínimo 8 caracteres, con letras y números", estado.errorClave)
        assertEquals("Repite tu contraseña", estado.errorRepetirClave)
        assertEquals("Debes aceptar los términos", estado.errorTerminos)
        assertEquals(5, estado.camposConError)
        //el foco va al primero
        assertEquals(CampoRegistro.USERNAME, estado.campoConError)
        assertEquals(1, estado.intentoEnvio)
        assertFalse(estado.registrado)
        assertEquals(0, usuarios.registros)
    }

    @Test
    fun soloFaltanLosTerminos() {
        llenar(terminos = false)
        registro.onEvent(RegistroUiEvent.OnRegistrar)

        assertEquals(CampoRegistro.TERMINOS, registro.estado.value.campoConError)
        assertEquals(0, usuarios.registros)
    }

    @Test
    fun registroNormalIniciaSesion() = runBlocking {
        llenar()
        registro.onEvent(RegistroUiEvent.OnRegistrar)

        assertTrue(registro.estado.value.registrado)
        //el correo se guarda en minúsculas
        assertEquals("nueva.cuenta@ejemplo.cl", usuarios.usuarioActual().first()?.correo)
    }

    @Test
    fun pedirSerCreadorDejaLaCuentaEnRevisionSinSesion() = runBlocking {
        llenar(creador = true)
        registro.onEvent(RegistroUiEvent.OnRegistrar)

        assertTrue(registro.estado.value.registrado)
        assertTrue(registro.estado.value.quiereSerCreador)
        //RN-29: no entra a la app
        assertNull(usuarios.usuarioActual().first())
        val creada = usuarios.iniciarSesion("nueva.cuenta@ejemplo.cl", "Clave1234")
        assertNotNull(creada)
        assertEquals(EstadoSolicitud.PENDIENTE, creada?.solicitudCreador)
        assertNull(usuarios.usuarioActual().first())
    }

    @Test
    fun nombreOCorreoYaUsados() {
        registro.onEvent(RegistroUiEvent.OnUsernameChange("camila_explora"))
        registro.onEvent(RegistroUiEvent.OnCorreoChange("camila.explora@ejemplo.cl"))
        registro.onEvent(RegistroUiEvent.OnClaveChange("Clave1234"))
        registro.onEvent(RegistroUiEvent.OnRepetirClaveChange("Clave1234"))
        registro.onEvent(RegistroUiEvent.OnTerminosChange(true))
        registro.onEvent(RegistroUiEvent.OnRegistrar)

        assertEquals("Ese nombre ya está en uso", registro.estado.value.errorUsername)
        assertEquals("Ese correo ya está registrado", registro.estado.value.errorCorreo)
        assertEquals(0, usuarios.registros)
    }
}
