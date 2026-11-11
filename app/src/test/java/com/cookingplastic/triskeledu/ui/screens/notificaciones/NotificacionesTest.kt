package com.cookingplastic.triskeledu.ui.screens.notificaciones

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.location.FakeUbicacionRepository
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.notificaciones.AvisoLocal
import com.cookingplastic.triskeledu.data.notificaciones.GeneradorNotificaciones
import com.cookingplastic.triskeledu.data.notificaciones.ReglaAvisos
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakeNotificacionRepository
import com.cookingplastic.triskeledu.data.repository.FakePreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.navigation.PedidoMapa
import com.cookingplastic.triskeledu.ui.screens.mapa.MapaUiEvent
import com.cookingplastic.triskeledu.ui.screens.mapa.MapaViewModel
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
import java.time.LocalDateTime
import java.time.ZoneId

//PER-03, PER-04 y RN-24: historial, contador y reglas de las notificaciones locales
@OptIn(ExperimentalCoroutinesApi::class)
class NotificacionesTest {

    private val zona = ZoneId.of("America/Santiago")
    //un martes a las 11:30 en Santiago
    private val ahora = LocalDateTime.of(2026, 11, 3, 11, 30).atZone(zona).toInstant().toEpochMilli()
    private val unDia = 24 * 60 * 60 * 1000L
    private var reloj = ahora
    private val camila = DatosPrueba.USUARIO_CAMILA
    private val laMoneda = DatosPrueba.PUNTO_LA_MONEDA
    private lateinit var usuarios: FakeUsuarioRepository
    private lateinit var preferencias: FakePreferenciasRepository
    private lateinit var notificaciones: FakeNotificacionRepository
    private val avisos = mutableListOf<String>()
    private lateinit var generador: GeneradorNotificaciones

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        usuarios = FakeUsuarioRepository()
        preferencias = FakePreferenciasRepository()
        notificaciones = FakeNotificacionRepository()
        reloj = ahora
        generador = GeneradorNotificaciones(
            notificaciones,
            object : AvisoLocal { override fun mostrar(tipo: TipoNotificacion, titulo: String, texto: String) { avisos += titulo } },
            usuarios,
            preferencias,
            ahora = { reloj },
            zona = zona
        )
        runBlocking { usuarios.iniciarSesion(camila.correo, DatosPrueba.CLAVE_DE_PRUEBA) }
    }

    @After
    fun despues() = Dispatchers.resetMain()

    private fun aviso(tipo: TipoNotificacion, haceMs: Long, leida: Boolean = false, puntoId: String? = null) =
        Notificacion(usuarioId = camila.id, tipo = tipo, titulo = tipo.name, texto = "", puntoId = puntoId, creadaEn = ahora - haceMs, leida = leida)

    @Test
    fun lugarCercanoUnaVezAlDiaPorPunto() = runBlocking {
        generador.lugarCercano(camila.id, laMoneda, 150.0)
        generador.lugarCercano(camila.id, laMoneda, 120.0)
        assertEquals(1, notificaciones.todas.size)
        assertEquals("Estás a 150 m de ${laMoneda.nombre}. ¡Búscalo!", notificaciones.todas.single().texto)

        //otro punto el mismo día sí avisa
        generador.lugarCercano(camila.id, DatosPrueba.PUNTO_CATEDRAL, 90.0)
        assertEquals(2, notificaciones.todas.size)

        //al día siguiente, La Moneda puede avisar de nuevo
        reloj = ahora + unDia
        generador.lugarCercano(camila.id, laMoneda, 150.0)
        assertEquals(3, notificaciones.todas.size)
    }

    @Test
    fun laReglaDelDiaYElRadio() = runBlocking {
        val historial = listOf(aviso(TipoNotificacion.LUGAR_CERCANO, haceMs = 2 * 60 * 60 * 1000L, puntoId = laMoneda.id))
        assertTrue(ReglaAvisos.yaAvisadoHoy(historial, laMoneda.id, ahora, zona))
        assertFalse(ReglaAvisos.yaAvisadoHoy(historial, DatosPrueba.PUNTO_CATEDRAL.id, ahora, zona))
        assertFalse(ReglaAvisos.yaAvisadoHoy(historial, laMoneda.id, ahora + unDia, zona))

        //a más de 200 m no se avisa
        generador.lugarCercano(camila.id, laMoneda, 201.0)
        assertTrue(notificaciones.todas.isEmpty())
    }

    @Test
    fun seRespetaElInterruptorDeConfiguracion() = runBlocking {
        preferencias.guardar(Preferencias(usuarioId = camila.id, notifLugaresCercanos = false))
        generador.lugarCercano(camila.id, laMoneda, 50.0)
        assertTrue(notificaciones.todas.isEmpty())
        assertTrue(avisos.isEmpty())
    }

    @Test
    fun cadaAvisoLeLlegaAQuienCorresponde() = runBlocking {
        //"Nueva cápsula": solo a quien tiene el aviso encendido (por defecto viene apagado)
        preferencias.guardar(Preferencias(usuarioId = camila.id, notifNuevasCapsulas = true))
        generador.nuevaCapsula(DatosPrueba.PUNTO_PARQUE_ALMAGRO)
        assertEquals(listOf(camila.id), notificaciones.todas.map { it.usuarioId })

        //al administrador: punto listo para activar; no es el usuario conectado, así que solo queda en su historial
        avisos.clear()
        generador.puntoListo(DatosPrueba.PUNTO_PALACIO_MATTE, DatosPrueba.USUARIO_CONTENIDO)
        val listo = notificaciones.todas.single { it.tipo == TipoNotificacion.PUNTO_LISTO }
        assertEquals(DatosPrueba.USUARIO_ADMIN.id, listo.usuarioId)
        assertTrue(avisos.isEmpty())
    }

    @Test
    fun contadorYMarcarLeidas() = runBlocking {
        notificaciones.insertar(aviso(TipoNotificacion.LUGAR_CERCANO, haceMs = 60_000, puntoId = laMoneda.id))
        notificaciones.insertar(aviso(TipoNotificacion.LOGRO, haceMs = 120_000))
        notificaciones.insertar(aviso(TipoNotificacion.NUEVA_CAPSULA, haceMs = 3 * unDia, leida = true, puntoId = laMoneda.id))
        val historial = NotificacionesViewModel(usuarios, notificaciones, ahora = { ahora }, zona = zona)
        assertEquals(2, historial.estado.value.sinLeer)
        assertEquals(3, historial.estado.value.filas.size)

        //tocar una la marca leída y abre su lugar
        val cercana = historial.estado.value.filas.first().notificacion
        historial.onEvent(NotificacionesUiEvent.OnTocar(cercana))
        assertEquals(1, historial.estado.value.sinLeer)
        assertEquals(DestinoNotificacion.Detalle(laMoneda.id), historial.estado.value.destino)

        historial.onEvent(NotificacionesUiEvent.OnMarcarTodasLeidas)
        assertEquals(0, historial.estado.value.sinLeer)
    }

    @Test
    fun elMapaMuestraElContadorYAvisaLosLugaresCercanos() = runBlocking {
        //usuaria_nueva no ha escaneado nada y está a 35 m de La Moneda
        usuarios.iniciarSesion(DatosPrueba.USUARIO_NUEVA.correo, DatosPrueba.CLAVE_DE_PRUEBA)
        notificaciones.insertar(aviso(TipoNotificacion.LOGRO, haceMs = 1000).copy(usuarioId = DatosPrueba.USUARIO_NUEVA.id))
        val mapa = MapaViewModel(
            FakePuntoInteresRepository(), FakeUbicacionRepository(), usuarios, preferencias,
            FakeProgresoRepository(DatosPrueba.progreso), PedidoMapa(), FakeCapsulaRepository(), notificaciones, generador
        )
        assertEquals(1, mapa.estado.value.notificacionesSinLeer)

        mapa.onEvent(MapaUiEvent.OnPermisoUbicacion(true))

        val cercana = notificaciones.todas.single { it.tipo == TipoNotificacion.LUGAR_CERCANO && it.puntoId == laMoneda.id }
        assertEquals(DatosPrueba.USUARIO_NUEVA.id, cercana.usuarioId)
        assertTrue(mapa.estado.value.notificacionesSinLeer >= 2)
    }

    @Test
    fun gruposYFechasDelHistorial() {
        val lista = listOf(
            aviso(TipoNotificacion.LUGAR_CERCANO, haceMs = 60 * 60 * 1000L),
            aviso(TipoNotificacion.NUEVA_CAPSULA, haceMs = 2 * unDia),
            aviso(TipoNotificacion.LOGRO, haceMs = 10 * unDia)
        )
        val filas = armarNotificaciones(lista, ahora, zona)

        assertEquals(listOf(GrupoNotificacion.HOY, GrupoNotificacion.ESTA_SEMANA, GrupoNotificacion.ANTES), filas.map { it.grupo })
        assertEquals("10:30", filas[0].fecha)
        //hace dos días desde un martes: domingo
        assertEquals("Domingo", filas[1].fecha)
        assertEquals("24 oct", filas[2].fecha)
    }

    @Test
    fun queAbreCadaNotificacion() {
        assertEquals(DestinoNotificacion.Nivel, destinoDe(aviso(TipoNotificacion.LOGRO, 0), RolUsuario.CLIENTE))
        assertEquals(DestinoNotificacion.Detalle(laMoneda.id), destinoDe(aviso(TipoNotificacion.LOGRO, 0, puntoId = laMoneda.id), RolUsuario.CLIENTE))
        assertEquals(DestinoNotificacion.EditarPunto(laMoneda.id), destinoDe(aviso(TipoNotificacion.PUNTO_LISTO, 0, puntoId = laMoneda.id), RolUsuario.ADMINISTRADOR))
        assertEquals(DestinoNotificacion.Solicitudes, destinoDe(aviso(TipoNotificacion.SOLICITUD, 0), RolUsuario.ADMINISTRADOR))
        assertNull(destinoDe(aviso(TipoNotificacion.SOLICITUD, 0), RolUsuario.CREADOR))
        assertNull(destinoDe(aviso(TipoNotificacion.SISTEMA, 0), RolUsuario.CLIENTE))
    }
}
