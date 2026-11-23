package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.location.FakeUbicacionRepository
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakePreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.navigation.Destination
import com.cookingplastic.triskeledu.navigation.PedidoMapa
import com.cookingplastic.triskeledu.ui.event.AdminEditarUiEvent
import com.cookingplastic.triskeledu.ui.event.AdminListaUiEvent
import com.cookingplastic.triskeledu.ui.validation.ReglaCapsula
import com.cookingplastic.triskeledu.ui.validation.ReglaMapa
import com.cookingplastic.triskeledu.ui.validation.ReglaPunto
import com.cookingplastic.triskeledu.ui.validation.ReglaQr
import com.cookingplastic.triskeledu.ui.viewmodels.AdminEditarViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.AdminListaViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.MapaViewModel
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

//Etapa 7: P07-Admin, P10 y el mapa del administrador (RN-14, RN-15, RN-27, RN-30, ADM-07, ADM-08, ADM-09, MAP-01)
@OptIn(ExperimentalCoroutinesApi::class)
class AdminPuntosTest {

    private lateinit var capsulas: FakeCapsulaRepository
    private lateinit var puntos: FakePuntoInteresRepository
    private lateinit var progreso: FakeProgresoRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        capsulas = FakeCapsulaRepository()
        puntos = FakePuntoInteresRepository(capsulaRepository = capsulas)
        progreso = FakeProgresoRepository(DatosPrueba.progreso)
    }

    @After
    fun despues() = Dispatchers.resetMain()

    private fun lista() = AdminListaViewModel(puntos, capsulas, progreso)
    private fun punto(id: String) = runBlocking { puntos.obtenerPorId(id).first() }

    @Test
    fun soloSeEliminaUnPuntoSinProgreso() {
        val admin = lista()

        //RN-30: La Moneda ya tiene escaneos: no se abre el diálogo y se explica por qué
        admin.onEvent(AdminListaUiEvent.OnPedirEliminar(DatosPrueba.PUNTO_LA_MONEDA))
        assertNull(admin.estado.value.puntoAEliminar)
        assertEquals(ReglaPunto.ELIMINAR_CON_PROGRESO, admin.estado.value.mensaje)
        assertTrue(admin.estado.value.escaneosDe(DatosPrueba.PUNTO_LA_MONEDA) > 0)

        //Padre Alonso de Ovalle no lo escaneó nadie: se elimina con confirmación
        val ovalle = DatosPrueba.PUNTO_PADRE_ALONSO_OVALLE
        admin.onEvent(AdminListaUiEvent.OnPedirEliminar(ovalle))
        assertEquals(ovalle.id, admin.estado.value.puntoAEliminar?.id)
        admin.onEvent(AdminListaUiEvent.OnConfirmarEliminar)
        assertNull(punto(ovalle.id))
        assertEquals(DatosPrueba.PUNTO_LA_MONEDA.id, punto(DatosPrueba.PUNTO_LA_MONEDA.id)?.id)
    }

    @Test
    fun alEliminarElPuntoSuCapsulaQuedaLibre() = runBlocking {
        val ovalle = DatosPrueba.PUNTO_PADRE_ALONSO_OVALLE
        assertFalse(ReglaCapsula.capsulasParaPunto(capsulas.capsulas().first(), puntos.obtenerTodos().first(), null).any { it.id == ovalle.capsulaId })

        val admin = lista()
        admin.onEvent(AdminListaUiEvent.OnPedirEliminar(ovalle))
        admin.onEvent(AdminListaUiEvent.OnConfirmarEliminar)

        assertTrue(ReglaCapsula.capsulasParaPunto(capsulas.capsulas().first(), puntos.obtenerTodos().first(), null).any { it.id == ovalle.capsulaId })
    }

    @Test
    fun elInterruptorQuedaBloqueadoConSinCapsula() {
        val admin = lista()
        val matte = DatosPrueba.PUNTO_PALACIO_MATTE
        assertEquals(EstadoAdminPunto.SIN_CAPSULA, admin.estado.value.estadoDe(matte))
        assertTrue(ReglaPunto.interruptorBloqueado(EstadoAdminPunto.SIN_CAPSULA))
        assertFalse(ReglaPunto.interruptorBloqueado(EstadoAdminPunto.INACTIVO))

        admin.onEvent(AdminListaUiEvent.OnCambiarActivo(matte.id, true))

        assertEquals(ReglaPunto.ACTIVAR_SIN_CAPSULA, admin.estado.value.mensaje)
        assertFalse(punto(matte.id)!!.activo)
    }

    @Test
    fun losPuntosDePruebaSeActivanConSuCapsulaSinEnlace() {
        //excepción de demo (RN-14 y RN-35): Parque Almagro tiene su cápsula de prueba sin enlace
        val admin = lista()
        val almagro = DatosPrueba.PUNTO_PARQUE_ALMAGRO
        admin.onEvent(AdminListaUiEvent.OnCambiarActivo(almagro.id, true))
        assertTrue(punto(almagro.id)!!.activo)
        //y se puede volver a desactivar sin borrar el progreso de nadie
        admin.onEvent(AdminListaUiEvent.OnCambiarActivo(almagro.id, false))
        assertFalse(punto(almagro.id)!!.activo)
        assertTrue(runBlocking { progreso.escaneosPorPunto().first() }.isNotEmpty())
    }

    @Test
    fun unPuntoNuevoSinEnlaceNoSeActiva() = runBlocking {
        //una cápsula publicada que no es de los datos de prueba necesita su enlace para activar el punto
        val sinEnlace = DatosPrueba.CAPSULA_LIBRE.copy(id = "nueva-sin-enlace", urlStream = null)
        val conEnlace = DatosPrueba.CAPSULA_LIBRE.copy(id = "nueva-con-enlace", urlStream = "https://drive.google.com/file/d/abc/view")
        capsulas.guardar(sinEnlace)
        capsulas.guardar(conEnlace)
        val matte = DatosPrueba.PUNTO_PALACIO_MATTE
        puntos.guardar(matte.copy(capsulaId = sinEnlace.id))
        val admin = lista()

        admin.onEvent(AdminListaUiEvent.OnCambiarActivo(matte.id, true))
        assertFalse(punto(matte.id)!!.activo)

        puntos.guardar(matte.copy(capsulaId = conEnlace.id))
        admin.onEvent(AdminListaUiEvent.OnCambiarActivo(matte.id, true))
        assertTrue(punto(matte.id)!!.activo)
    }

    @Test
    fun filtrosDeMisLugaresDelAdmin() {
        val admin = lista()
        admin.onEvent(AdminListaUiEvent.OnFiltroChange(EstadoAdminPunto.INACTIVO))
        assertEquals(listOf(DatosPrueba.PUNTO_PARQUE_ALMAGRO.id), admin.estado.value.puntosFiltrados.map { it.id })
        admin.onEvent(AdminListaUiEvent.OnFiltroChange(EstadoAdminPunto.SIN_CAPSULA))
        assertEquals(listOf(DatosPrueba.PUNTO_PALACIO_MATTE.id), admin.estado.value.puntosFiltrados.map { it.id })
        admin.onEvent(AdminListaUiEvent.OnFiltroChange(null))
        assertEquals(16, admin.estado.value.puntosFiltrados.size)
        assertEquals(14, admin.estado.value.totalActivos)
    }

    @Test
    fun elSelectorDeCapsulasSoloOfrecePublicadasYLibres() {
        //ADM-07: de cualquier creador; sin borradores ni las que ya están en otro punto
        val deOtroCreador = DatosPrueba.CAPSULA_LIBRE.copy(id = "otra", titulo = "Otra libre", creadorId = DatosPrueba.USUARIO_ADMIN.id)
        val lista = DatosPrueba.capsulas + deOtroCreador
        val paraNuevo = ReglaCapsula.capsulasParaPunto(lista, DatosPrueba.puntos, null)
        assertEquals(setOf(DatosPrueba.CAPSULA_LIBRE.id, deOtroCreador.id), paraNuevo.map { it.id }.toSet())
        assertTrue(paraNuevo.all { it.estado == EstadoCapsula.PUBLICADA })

        //al editar La Moneda también aparece su propia cápsula
        val paraLaMoneda = ReglaCapsula.capsulasParaPunto(lista, DatosPrueba.puntos, DatosPrueba.PUNTO_LA_MONEDA.id)
        assertTrue(paraLaMoneda.any { it.id == DatosPrueba.CAPSULA_LA_MONEDA.id })

        //el formulario usa el mismo filtro
        val formulario = AdminEditarViewModel(capsulas, puntos, FakeUbicacionRepository())
        assertEquals(listOf(DatosPrueba.CAPSULA_LIBRE.id), formulario.estado.value.capsulas.map { it.id })
    }

    @Test
    fun elQrSeGeneraAlGuardar() = runBlocking {
        //ADM-08: el siguiente al mayor que existe, con el formato de los datos de prueba
        assertEquals("TRISKEL-PI-001", ReglaQr.siguienteCodigo(emptyList()))
        assertEquals("TRISKEL-PI-017", ReglaQr.siguienteCodigo(DatosPrueba.puntos.map { it.codigoQr }))
        assertEquals("TRISKEL-PI-010", ReglaQr.siguienteCodigo(listOf("TRISKEL-PI-009", "otro-codigo", "TRISKEL-PI-002")))

        val formulario = AdminEditarViewModel(capsulas, puntos, FakeUbicacionRepository())
        assertNull(formulario.estado.value.codigoQr)
        formulario.onEvent(AdminEditarUiEvent.OnNombreChange("Barrio Lastarria"))
        formulario.onEvent(AdminEditarUiEvent.OnTipoChange(TipoPuntoInteres.ZCH))
        formulario.onEvent(AdminEditarUiEvent.OnImagenElegida("content://foto"))
        //P19: elegir en el mapa llena latitud y longitud; la altitud de un punto nuevo queda en 0
        formulario.onEvent(AdminEditarUiEvent.OnUbicacionElegida(-33.4372, -70.6395))
        assertEquals("0", formulario.estado.value.altitud)

        formulario.onEvent(AdminEditarUiEvent.OnGuardar)

        assertTrue(formulario.estado.value.guardado)
        val guardado = puntos.obtenerTodos().first().single { it.nombre == "Barrio Lastarria" }
        assertEquals("TRISKEL-PI-017", guardado.codigoQr)
        //sin cápsula queda Sin cápsula e inactivo (RN-22)
        assertNull(guardado.capsulaId)
        assertFalse(guardado.activo)
    }

    @Test
    fun alEditarSeConservaElQr() = runBlocking {
        val formulario = AdminEditarViewModel(capsulas, puntos, FakeUbicacionRepository(), DatosPrueba.PUNTO_LA_MONEDA.id)
        assertEquals("TRISKEL-PI-001", formulario.estado.value.codigoQr)
        formulario.onEvent(AdminEditarUiEvent.OnNombreChange("Palacio de La Moneda (editado)"))
        formulario.onEvent(AdminEditarUiEvent.OnGuardar)
        assertEquals("TRISKEL-PI-001", punto(DatosPrueba.PUNTO_LA_MONEDA.id)!!.codigoQr)
    }

    @Test
    fun elMapaFiltraPorRol() = runBlocking {
        //RN-15: el cliente nunca ve Inactivos ni Sin cápsula
        assertFalse(ReglaMapa.puntoVisible(EstadoAdminPunto.INACTIVO, RolUsuario.CLIENTE, mostrarInactivos = true))
        assertFalse(ReglaMapa.puntoVisible(EstadoAdminPunto.SIN_CAPSULA, RolUsuario.CLIENTE, mostrarInactivos = true))
        assertFalse(ReglaMapa.puntoVisible(EstadoAdminPunto.INACTIVO, RolUsuario.CREADOR, mostrarInactivos = true))
        assertTrue(ReglaMapa.puntoVisible(EstadoAdminPunto.INACTIVO, RolUsuario.ADMINISTRADOR, mostrarInactivos = true))
        assertFalse(ReglaMapa.puntoVisible(EstadoAdminPunto.INACTIVO, RolUsuario.ADMINISTRADOR, mostrarInactivos = false))

        val usuarios = FakeUsuarioRepository()
        val preferencias = FakePreferenciasRepository()
        val almagro = DatosPrueba.PUNTO_PARQUE_ALMAGRO.id
        fun mapa() = MapaViewModel(puntos, FakeUbicacionRepository(), usuarios, preferencias, progreso, PedidoMapa(), capsulas)

        usuarios.iniciarSesion(DatosPrueba.USUARIO_CAMILA.correo, DatosPrueba.CLAVE_DE_PRUEBA)
        assertFalse(mapa().estado.value.puntos.any { it.punto.id == almagro })

        //ADM-11: el administrador ve el inactivo con su leyenda
        usuarios.iniciarSesion(DatosPrueba.USUARIO_ADMIN.correo, DatosPrueba.CLAVE_DE_PRUEBA)
        val delAdmin = mapa()
        assertTrue(delAdmin.estado.value.puntos.single { it.punto.id == almagro }.inactivo)
        assertEquals(1, delAdmin.estado.value.puntosInactivos)
        assertEquals(1, delAdmin.estado.value.puntosSinCapsula)
        assertEquals(14, delAdmin.estado.value.totalLugares)

        //respeta "Mostrar puntos inactivos"
        preferencias.guardar(Preferencias(usuarioId = DatosPrueba.USUARIO_ADMIN.id, mostrarInactivos = false))
        assertFalse(delAdmin.estado.value.puntos.any { it.punto.id == almagro })
    }

    @Test
    fun elAdministradorNoTieneEscanear() {
        //MAP-01
        assertEquals(listOf(Destination.MAPA, Destination.MIS_LUGARES), Destination.pestanasPara(RolUsuario.ADMINISTRADOR))
        assertEquals(Destination.entries, Destination.pestanasPara(RolUsuario.CLIENTE))
        assertEquals(Destination.entries, Destination.pestanasPara(RolUsuario.CREADOR))
        assertEquals("Mis lugares", Destination.etiquetaPara(Destination.MIS_LUGARES, RolUsuario.ADMINISTRADOR))
        assertEquals("Mis descubrimientos", Destination.etiquetaPara(Destination.MIS_LUGARES, RolUsuario.CLIENTE))
    }
}
