package com.cookingplastic.triskeledu.ui.screens.creador

import com.cookingplastic.triskeledu.data.creador.GestorCapsulas
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.CategoriaCapsula
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.domain.rules.ValidacionCapsula
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

//CRE-02, CRE-03, CRE-04 y CRE-06: validaciones de P18, vista previa y lista de P17
@OptIn(ExperimentalCoroutinesApi::class)
class ValidacionCapsulaTest {

    private val enlace = "https://drive.google.com/file/d/1aBcD-eF_9/view?usp=sharing"
    private lateinit var capsulas: FakeCapsulaRepository
    private lateinit var usuarios: FakeUsuarioRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        capsulas = FakeCapsulaRepository()
        usuarios = FakeUsuarioRepository()
        runBlocking { usuarios.iniciarSesion(DatosPrueba.USUARIO_CONTENIDO.correo, DatosPrueba.CLAVE_DE_PRUEBA) }
    }

    @After
    fun despues() = Dispatchers.resetMain()

    private fun editor(capsulaId: String? = null) =
        CapsulaEditarViewModel(capsulaId, usuarios, capsulas, GestorCapsulas(capsulas, FakePuntoInteresRepository(capsulaRepository = capsulas)))

    @Test
    fun elEnlaceDeDriveEsObligatorio() {
        assertEquals("Pega un enlace de Google Drive", ValidacionCapsula.validarEnlace(""))
        assertEquals("Pega un enlace de Google Drive", ValidacionCapsula.validarEnlace("https://youtube.com/watch?v=123"))
        assertEquals("Pega un enlace de Google Drive", ValidacionCapsula.validarEnlace("https://drive.google.com/drive/folders/1aB"))
        assertNull(ValidacionCapsula.validarEnlace(enlace))
        assertNull(ValidacionCapsula.validarEnlace("https://drive.google.com/open?id=1aBcD"))
    }

    @Test
    fun laDuracionVaDe1A180Segundos() {
        assertEquals("Ingresa la duración en segundos", ValidacionCapsula.validarDuracion(""))
        assertEquals("Ingresa la duración en segundos", ValidacionCapsula.validarDuracion("0"))
        assertEquals("La cápsula debe durar 3 minutos como máximo", ValidacionCapsula.validarDuracion("181"))
        assertNull(ValidacionCapsula.validarDuracion("1"))
        assertNull(ValidacionCapsula.validarDuracion("180"))
    }

    @Test
    fun tituloDescripcionYCategoria() {
        assertEquals("El título es obligatorio", ValidacionCapsula.validarTitulo("   "))
        assertEquals("El título debe tener entre 3 y 80 caracteres", ValidacionCapsula.validarTitulo("IA"))
        assertEquals("El título debe tener entre 3 y 80 caracteres", ValidacionCapsula.validarTitulo("a".repeat(81)))
        assertNull(ValidacionCapsula.validarTitulo("¿Qué es la IA?"))
        assertEquals("Máximo 300 caracteres", ValidacionCapsula.validarDescripcion("a".repeat(301)))
        assertNull(ValidacionCapsula.validarDescripcion(""))
        assertEquals("Elige una categoría", ValidacionCapsula.validarCategoria(null))
    }

    @Test
    fun guardarVacioMuestraLosErroresYNoGuarda() = runBlocking {
        val antes = capsulas.capsulas().first().size
        val pantalla = editor()

        pantalla.onEvent(CapsulaEditarUiEvent.OnGuardar)

        val estado = pantalla.estado.value
        assertEquals("El título es obligatorio", estado.errorTitulo)
        assertEquals("Elige una categoría", estado.errorCategoria)
        assertEquals("Pega un enlace de Google Drive", estado.errorEnlace)
        assertEquals("Ingresa la duración en segundos", estado.errorDuracion)
        //RN-18: el foco va al primero
        assertEquals(CampoCapsula.TITULO, estado.campoConError)
        assertEquals(antes, capsulas.capsulas().first().size)
    }

    @Test
    fun unaCapsulaNuevaSeGuardaComoBorrador() = runBlocking {
        val pantalla = editor()
        pantalla.onEvent(CapsulaEditarUiEvent.OnTituloChange("Algoritmos en el Mercado"))
        pantalla.onEvent(CapsulaEditarUiEvent.OnCategoriaChange(CategoriaCapsula.IA_VIDA_DIARIA))
        pantalla.onEvent(CapsulaEditarUiEvent.OnEnlaceChange(enlace))
        pantalla.onEvent(CapsulaEditarUiEvent.OnDuracionChange("52 s"))

        pantalla.onEvent(CapsulaEditarUiEvent.OnGuardar)

        assertTrue(pantalla.estado.value.terminada)
        val guardada = capsulas.capsulas().first().single { it.titulo == "Algoritmos en el Mercado" }
        assertEquals(EstadoCapsula.BORRADOR, guardada.estado)
        assertEquals(DatosPrueba.USUARIO_CONTENIDO.id, guardada.creadorId)
        assertEquals(52, guardada.duracionSegundos)
        assertNull(guardada.descripcion)
    }

    @Test
    fun editarYEliminarPidenConfirmacion() = runBlocking {
        val pantalla = editor(DatosPrueba.CAPSULA_BORRADOR.id)
        pantalla.onEvent(CapsulaEditarUiEvent.OnEnlaceChange(enlace))

        pantalla.onEvent(CapsulaEditarUiEvent.OnGuardar)
        assertEquals(ConfirmacionCapsula.GUARDAR, pantalla.estado.value.confirmacion)
        pantalla.onEvent(CapsulaEditarUiEvent.OnConfirmar)
        assertEquals(enlace, capsulas.obtenerPorId(DatosPrueba.CAPSULA_BORRADOR.id)!!.urlStream)

        val otra = editor(DatosPrueba.CAPSULA_BORRADOR.id)
        otra.onEvent(CapsulaEditarUiEvent.OnEliminar)
        assertEquals(ConfirmacionCapsula.ELIMINAR, otra.estado.value.confirmacion)
        otra.onEvent(CapsulaEditarUiEvent.OnConfirmar)
        assertNull(capsulas.obtenerPorId(DatosPrueba.CAPSULA_BORRADOR.id))
    }

    @Test
    fun probarVideoGuardaYAbreLaVistaPrevia() {
        val pantalla = editor(DatosPrueba.CAPSULA_BORRADOR.id)
        pantalla.onEvent(CapsulaEditarUiEvent.OnEnlaceChange(enlace))

        pantalla.onEvent(CapsulaEditarUiEvent.OnProbarVideo)

        assertEquals(DatosPrueba.CAPSULA_BORRADOR.id, pantalla.estado.value.abrirVistaPrevia)
    }

    @Test
    fun soloSePublicaUnVideoQueCargoYDuraHasta3Minutos() {
        assertNotNull(motivoNoPublicarVideo(videoCargado = false, duracionMs = 0))
        assertEquals("El video dura más de 3 minutos: acórtalo antes de publicarlo", motivoNoPublicarVideo(true, 200_000))
        assertNull(motivoNoPublicarVideo(true, 52_000))
    }

    @Test
    fun misCapsulasBuscaYFiltra() {
        val propias = DatosPrueba.capsulas.filter { it.creadorId == DatosPrueba.USUARIO_CONTENIDO.id }

        //la búsqueda no distingue mayúsculas ni tildes
        val busqueda = armarCapsulas(propias, DatosPrueba.puntos, "TRANSITO", FiltroEstadoCapsula.TODAS, null)
        assertEquals(listOf(DatosPrueba.CAPSULA_LIBRE.id), busqueda.capsulas.map { it.capsula.id })
        assertNull(busqueda.capsulas.single().nombrePunto)

        val borradores = armarCapsulas(propias, DatosPrueba.puntos, "", FiltroEstadoCapsula.BORRADORES, null)
        assertEquals(listOf(DatosPrueba.CAPSULA_BORRADOR.id), borradores.capsulas.map { it.capsula.id })

        //CRE-04: cada una dice si ya está asociada a un punto
        val ciudad = armarCapsulas(propias, DatosPrueba.puntos, "", FiltroEstadoCapsula.PUBLICADAS, CategoriaCapsula.IA_CIUDAD)
        assertTrue(ciudad.capsulas.all { it.capsula.categoria == CategoriaCapsula.IA_CIUDAD })
        assertEquals(propias.size, ciudad.totalPropias)
    }
}
