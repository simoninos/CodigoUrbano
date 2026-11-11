package com.cookingplastic.triskeledu.ui.screens.solicitudes

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.local.dao.NotificacionDao
import com.cookingplastic.triskeledu.data.location.FakeUbicacionRepository
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.notificaciones.AvisoLocal
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakePreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.data.simulacion.ResultadoSolicitud
import com.cookingplastic.triskeledu.data.simulacion.SolicitudesSimuladas
import com.cookingplastic.triskeledu.navigation.PedidoMapa
import com.cookingplastic.triskeledu.domain.rules.ReglaMapa
import com.cookingplastic.triskeledu.ui.screens.mapa.MapaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

//ADM-10, SIM-02, RN-29 y EXC-09: solicitudes de creador; CRE-07 y RN-15: puntos Sin cápsula en el mapa
@OptIn(ExperimentalCoroutinesApi::class)
class SolicitudesTest {

    private class NotificacionesEnMemoria : NotificacionDao {
        val lista = mutableListOf<Notificacion>()
        override fun notificacionesDe(usuarioId: String) = MutableStateFlow(lista.filter { it.usuarioId == usuarioId })
        override fun sinLeer(usuarioId: String) = MutableStateFlow(lista.count { it.usuarioId == usuarioId && !it.leida })
        override suspend fun insertar(notificacion: Notificacion) { lista += notificacion }
        override suspend fun marcarLeida(id: String) = Unit
        override suspend fun marcarTodasLeidas(usuarioId: String) = Unit
    }

    private class AvisoAnotado : AvisoLocal {
        val titulos = mutableListOf<String>()
        override fun mostrar(tipo: TipoNotificacion, titulo: String, texto: String) { titulos += titulo }
    }

    private val ahora = 1_800_000_000_000L
    private val pendiente = DatosPrueba.USUARIO_PENDIENTE
    private val admin = DatosPrueba.USUARIO_ADMIN
    private lateinit var usuarios: FakeUsuarioRepository
    private lateinit var notificaciones: NotificacionesEnMemoria
    private lateinit var avisos: AvisoAnotado
    private lateinit var solicitudes: SolicitudesSimuladas

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        usuarios = FakeUsuarioRepository()
        notificaciones = NotificacionesEnMemoria()
        avisos = AvisoAnotado()
        solicitudes = SolicitudesSimuladas(usuarios, notificaciones, avisos) { ahora }
    }

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun aprobarLaDejaComoCreador() = runBlocking {
        assertEquals(listOf(pendiente.id), solicitudes.pendientes().first().map { it.id })

        assertEquals(ResultadoSolicitud.RESUELTA, solicitudes.aprobar(pendiente.id, admin.id))

        assertTrue(solicitudes.pendientes().first().isEmpty())
        //RN-29: ya no está bloqueada: inicia sesión con su rol nuevo
        val usuario = usuarios.iniciarSesion(pendiente.correo, DatosPrueba.CLAVE_DE_PRUEBA)!!
        assertEquals(RolUsuario.CREADOR, usuario.rol)
        assertEquals(EstadoSolicitud.APROBADA, usuario.solicitudCreador)
        assertEquals(ahora, usuario.solicitudResueltaEn)
        assertEquals(admin.id, usuario.solicitudResueltaPor)
        assertEquals(pendiente.id, usuarios.usuarioActual().first()?.id)
        //queda una notificación para ese usuario
        val aviso = notificaciones.lista.single()
        assertEquals(pendiente.id, aviso.usuarioId)
        assertEquals(TipoNotificacion.SOLICITUD, aviso.tipo)
        assertEquals(SolicitudesSimuladas.TITULO_APROBADA, aviso.titulo)
    }

    @Test
    fun rechazarLaDejaComoCliente() = runBlocking {
        assertEquals(ResultadoSolicitud.RESUELTA, solicitudes.rechazar(pendiente.id, admin.id))

        val usuario = usuarios.iniciarSesion(pendiente.correo, DatosPrueba.CLAVE_DE_PRUEBA)!!
        assertEquals(RolUsuario.CLIENTE, usuario.rol)
        assertEquals(EstadoSolicitud.RECHAZADA, usuario.solicitudCreador)
        assertEquals(SolicitudesSimuladas.TITULO_RECHAZADA, notificaciones.lista.single().titulo)
    }

    @Test
    fun unaSolicitudYaRespondidaNoSeRespondeDeNuevo() = runBlocking {
        solicitudes.aprobar(pendiente.id, admin.id)

        //EXC-09: otro administrador (o un segundo toque) llega tarde
        assertEquals(ResultadoSolicitud.YA_RESPONDIDA, solicitudes.rechazar(pendiente.id, admin.id))
        assertEquals(ResultadoSolicitud.YA_RESPONDIDA, solicitudes.aprobar(pendiente.id, admin.id))
        assertEquals(RolUsuario.CREADOR, usuarios.iniciarSesion(pendiente.correo, DatosPrueba.CLAVE_DE_PRUEBA)!!.rol)
        assertEquals(1, notificaciones.lista.size)
    }

    @Test
    fun alEntrarSeAvisaLaRespuesta() = runBlocking {
        solicitudes.aprobar(pendiente.id, admin.id)

        solicitudes.avisarRespuesta(pendiente.id)

        assertEquals(listOf(SolicitudesSimuladas.TITULO_APROBADA), avisos.titulos)
    }

    @Test
    fun laPantallaPideConfirmacionYAvisaSiYaFueRespondida() = runBlocking {
        usuarios.iniciarSesion(admin.correo, DatosPrueba.CLAVE_DE_PRUEBA)
        val pantalla = SolicitudesViewModel(solicitudes, usuarios)
        val solicitud = pantalla.estado.value.pendientes.single()

        pantalla.onEvent(SolicitudesUiEvent.OnAprobar(solicitud))
        assertNotNull(pantalla.estado.value.porConfirmar)
        pantalla.onEvent(SolicitudesUiEvent.OnConfirmar)
        assertTrue(pantalla.estado.value.pendientes.isEmpty())

        //la misma solicitud, respondida de nuevo desde una lista vieja
        pantalla.onEvent(SolicitudesUiEvent.OnRechazar(solicitud))
        pantalla.onEvent(SolicitudesUiEvent.OnConfirmar)
        assertEquals(SolicitudesViewModel.YA_RESPONDIDA, pantalla.estado.value.mensaje)
    }

    @Test
    fun elClienteNuncaVePuntosSinCapsula() {
        //RN-15
        assertFalse(ReglaMapa.puntoVisible(EstadoAdminPunto.SIN_CAPSULA, RolUsuario.CLIENTE))
        assertFalse(ReglaMapa.puntoVisible(EstadoAdminPunto.INACTIVO, RolUsuario.CLIENTE))
        assertFalse(ReglaMapa.puntoVisible(EstadoAdminPunto.INACTIVO, RolUsuario.CREADOR))
        assertTrue(ReglaMapa.puntoVisible(EstadoAdminPunto.SIN_CAPSULA, RolUsuario.CREADOR))
        assertTrue(ReglaMapa.puntoVisible(EstadoAdminPunto.SIN_CAPSULA, RolUsuario.ADMINISTRADOR))
    }

    @Test
    fun elMapaDelCreadorMuestraLosSinCapsula() = runBlocking {
        val matte = DatosPrueba.PUNTO_PALACIO_MATTE.id
        //cliente: solo los 14 activos
        usuarios.iniciarSesion(DatosPrueba.USUARIO_CAMILA.correo, DatosPrueba.CLAVE_DE_PRUEBA)
        val delCliente = nuevoMapa().estado.value
        assertFalse(delCliente.puntos.any { it.punto.id == matte })
        assertEquals(0, delCliente.puntosSinCapsula)

        //creador: además Palacio Matte, que no cuenta en "N de M lugares"
        usuarios.iniciarSesion(DatosPrueba.USUARIO_CONTENIDO.correo, DatosPrueba.CLAVE_DE_PRUEBA)
        val delCreador = nuevoMapa().estado.value
        assertTrue(delCreador.puntos.single { it.punto.id == matte }.sinCapsula)
        assertEquals(1, delCreador.puntosSinCapsula)
        assertEquals(14, delCreador.totalLugares)
        //Parque Almagro está Inactivo: el creador no lo ve
        assertFalse(delCreador.puntos.any { it.punto.id == DatosPrueba.PUNTO_PARQUE_ALMAGRO.id })
    }

    @Test
    fun laCampanaDelAdministradorCuentaLasSolicitudes() = runBlocking {
        usuarios.iniciarSesion(admin.correo, DatosPrueba.CLAVE_DE_PRUEBA)
        val mapa = nuevoMapa()
        assertEquals(1, mapa.estado.value.solicitudesPendientes)
        assertTrue(mapa.estado.value.esAdministrador)

        solicitudes.aprobar(pendiente.id, admin.id)
        assertEquals(0, mapa.estado.value.solicitudesPendientes)
    }

    private fun nuevoMapa() = MapaViewModel(
        FakePuntoInteresRepository(),
        FakeUbicacionRepository(),
        usuarios,
        FakePreferenciasRepository(),
        FakeProgresoRepository(DatosPrueba.progreso),
        PedidoMapa(),
        FakeCapsulaRepository()
    )
}
