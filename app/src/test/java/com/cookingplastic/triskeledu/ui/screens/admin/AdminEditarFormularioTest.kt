package com.cookingplastic.triskeledu.ui.screens.admin

import com.cookingplastic.triskeledu.data.location.FakeUbicacionRepository
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

//RN-18 en el formulario de punto (P10): al tocar Guardar vacío se ven todos los errores, también el de la imagen
@OptIn(ExperimentalCoroutinesApi::class)
class AdminEditarFormularioTest {

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun guardarVacioMuestraTodosLosErrores() {
        val vm = AdminEditarViewModel(FakeCapsulaRepository(), FakePuntoInteresRepository(), FakeUbicacionRepository())
        vm.onEvent(AdminEditarUiEvent.OnGuardar)
        val estado = vm.estado.value

        assertEquals("El nombre es obligatorio", estado.errorNombre)
        assertEquals("Elige un tipo", estado.errorTipo)
        assertEquals("Agrega una imagen del punto", estado.errorImagen)
        //ADM-03: sin coordenadas se pide elegir la ubicación, en vez de marcar cada campo
        assertEquals("Elige la ubicación del punto", estado.errorUbicacion)
        assertNull(estado.errorLatitud)
        assertNull(estado.errorLongitud)
        assertEquals("La altitud debe ser un número", estado.errorAltitud)
        assertEquals(CampoPunto.NOMBRE, estado.campoConError)
        assertFalse(estado.guardado)
    }
}
