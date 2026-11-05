package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.data.creador.GestorCapsulas
import com.cookingplastic.triskeledu.data.creador.ResultadoCapsula
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.ui.event.AsociarCapsulaUiEvent
import com.cookingplastic.triskeledu.ui.viewmodels.AsociarCapsulaViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.armarAsociar
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

//CRE-08 y RN-28: solo cápsulas propias, publicadas y libres, a un punto Sin cápsula; el punto queda Inactivo
@OptIn(ExperimentalCoroutinesApi::class)
class AsociarCapsulaTest {

    private val creador = DatosPrueba.USUARIO_CONTENIDO
    private val matte = DatosPrueba.PUNTO_PALACIO_MATTE
    //una cápsula publicada y libre, pero de otro creador
    private val ajena = DatosPrueba.CAPSULA_LIBRE.copy(id = "capsula-ajena", titulo = "De otra persona", creadorId = DatosPrueba.USUARIO_ADMIN.id)
    private lateinit var capsulas: FakeCapsulaRepository
    private lateinit var puntos: FakePuntoInteresRepository
    private lateinit var gestor: GestorCapsulas

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        capsulas = FakeCapsulaRepository(DatosPrueba.capsulas + ajena)
        puntos = FakePuntoInteresRepository(capsulaRepository = capsulas)
        gestor = GestorCapsulas(capsulas, puntos)
    }

    @After
    fun despues() = Dispatchers.resetMain()

    private fun punto(id: String) = runBlocking { puntos.obtenerPorId(id).first()!! }

    @Test
    fun soloSeEligenLasPropiasPublicadasYLibres() {
        val estado = armarAsociar(matte.id, creador, DatosPrueba.capsulas + ajena, DatosPrueba.puntos)
        val porId = estado.opciones.associateBy { it.capsula.id }

        assertNull(estado.motivoPunto)
        //la única elegible es la publicada sin punto, y aparece primero
        assertEquals(listOf(DatosPrueba.CAPSULA_LIBRE.id), estado.opciones.filter { it.elegible }.map { it.capsula.id })
        assertEquals(DatosPrueba.CAPSULA_LIBRE.id, estado.opciones.first().capsula.id)
        //las demás, deshabilitadas con su motivo
        assertEquals("Ya está asociada a ${DatosPrueba.PUNTO_LA_MONEDA.nombre}", porId.getValue(DatosPrueba.CAPSULA_LA_MONEDA.id).motivo)
        assertEquals("Es un borrador: publícala primero", porId.getValue(DatosPrueba.CAPSULA_BORRADOR.id).motivo)
        //las de otro creador ni siquiera aparecen
        assertFalse(ajena.id in porId)
    }

    @Test
    fun alAsociarElPuntoPasaAInactivo() = runBlocking {
        assertEquals(EstadoAdminPunto.SIN_CAPSULA, EstadoAdminPunto.desde(matte, null))

        assertEquals(ResultadoCapsula.Listo, gestor.asociar(DatosPrueba.CAPSULA_LIBRE.id, matte.id, creador))

        val asociado = punto(matte.id)
        assertEquals(DatosPrueba.CAPSULA_LIBRE.id, asociado.capsulaId)
        assertFalse(asociado.activo)
        assertEquals(EstadoAdminPunto.INACTIVO, EstadoAdminPunto.desde(asociado, capsulas.obtenerPorId(DatosPrueba.CAPSULA_LIBRE.id)))
    }

    @Test
    fun noSeAsociaLoQueNoCumple() = runBlocking {
        //borrador
        assertEquals(ResultadoCapsula.Error("Es un borrador: publícala primero"), gestor.asociar(DatosPrueba.CAPSULA_BORRADOR.id, matte.id, creador))
        //ya está en otro punto
        assertTrue(gestor.asociar(DatosPrueba.CAPSULA_LA_MONEDA.id, matte.id, creador) is ResultadoCapsula.Error)
        //de otro creador
        assertEquals(ResultadoCapsula.Error("Es de otro creador"), gestor.asociar(ajena.id, matte.id, creador))
        //el punto ya tiene cápsula
        assertEquals(
            ResultadoCapsula.Error("Este punto ya tiene una cápsula"),
            gestor.asociar(DatosPrueba.CAPSULA_LIBRE.id, DatosPrueba.PUNTO_LA_MONEDA.id, creador)
        )
        assertNull(punto(matte.id).capsulaId)
    }

    @Test
    fun elAdministradorAsociaCapsulasDeCualquierCreador() = runBlocking {
        val admin = DatosPrueba.USUARIO_ADMIN.copy(rol = RolUsuario.ADMINISTRADOR)
        assertEquals(ResultadoCapsula.Listo, gestor.asociar(DatosPrueba.CAPSULA_LIBRE.id, matte.id, admin))
    }

    @Test
    fun laPantallaNoDejaElegirLasDeshabilitadas() {
        val usuarios = FakeUsuarioRepository()
        runBlocking { usuarios.iniciarSesion(creador.correo, DatosPrueba.CLAVE_DE_PRUEBA) }
        val pantalla = AsociarCapsulaViewModel(matte.id, usuarios, capsulas, puntos, gestor)

        pantalla.onEvent(AsociarCapsulaUiEvent.OnElegir(DatosPrueba.CAPSULA_BORRADOR.id))
        assertNull(pantalla.estado.value.elegidaId)
        assertFalse(pantalla.estado.value.puedeAsociar)

        pantalla.onEvent(AsociarCapsulaUiEvent.OnElegir(DatosPrueba.CAPSULA_LIBRE.id))
        assertTrue(pantalla.estado.value.puedeAsociar)
        pantalla.onEvent(AsociarCapsulaUiEvent.OnAsociar)

        assertTrue(pantalla.estado.value.asociada)
        assertEquals(DatosPrueba.CAPSULA_LIBRE.id, punto(matte.id).capsulaId)
    }
}
