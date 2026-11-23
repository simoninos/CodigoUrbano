package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.local.dao.NotificacionDao
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.nivel.ControlNivel
import com.cookingplastic.triskeledu.data.nivel.ProgresoConNivel
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakePreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.ui.event.ConfiguracionUiEvent
import com.cookingplastic.triskeledu.ui.event.SubidaNivelUiEvent
import com.cookingplastic.triskeledu.ui.viewmodels.ConfiguracionViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.SubidaNivelViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

//LVL-02, LVL-03, LVL-05, RN-33 y RN-34: P25 al subir de nivel y los temas que se pueden elegir
@OptIn(ExperimentalCoroutinesApi::class)
class SubidaNivelTest {

    private class NotificacionesEnMemoria : NotificacionDao {
        val lista = mutableListOf<Notificacion>()
        override fun notificacionesDe(usuarioId: String) = MutableStateFlow(lista.filter { it.usuarioId == usuarioId })
        override fun sinLeer(usuarioId: String) = MutableStateFlow(lista.count { it.usuarioId == usuarioId && !it.leida })
        override suspend fun insertar(notificacion: Notificacion) { lista += notificacion }
        override suspend fun marcarLeida(id: String) = Unit
        override suspend fun marcarTodasLeidas(usuarioId: String) = Unit
    }

    private val camila = DatosPrueba.USUARIO_CAMILA
    private lateinit var usuarios: FakeUsuarioRepository
    private lateinit var preferencias: FakePreferenciasRepository
    private lateinit var notificaciones: NotificacionesEnMemoria
    private lateinit var base: FakeProgresoRepository
    private lateinit var control: ControlNivel
    private lateinit var progreso: ProgresoConNivel

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        usuarios = FakeUsuarioRepository()
        preferencias = FakePreferenciasRepository()
        notificaciones = NotificacionesEnMemoria()
    }

    @After
    fun despues() = Dispatchers.resetMain()

    //conecta a un usuario con su progreso y el nivel más alto que alcanzó
    private fun entrar(usuario: Usuario, nivelMaximo: Int, filas: List<ProgresoPunto> = DatosPrueba.progreso) = runBlocking {
        usuarios.iniciarSesion(usuario.correo, DatosPrueba.CLAVE_DE_PRUEBA)
        preferencias.guardar(Preferencias(usuarioId = usuario.id, nivelMaximoAlcanzado = nivelMaximo))
        base = FakeProgresoRepository(filas)
        control = ControlNivel(base, preferencias, usuarios, notificaciones)
        progreso = ProgresoConNivel(base, control)
    }

    private fun preferenciasActuales() = runBlocking { preferencias.preferencias().first() }

    @Test
    fun alCruzarUnUmbralNuevoApareceP25() = runBlocking {
        //Camila: 160 puntos, nivel 3
        entrar(camila, nivelMaximo = 3)

        //+10: 170, sigue en el 3
        progreso.registrarEscaneo(camila.id, DatosPrueba.PUNTO_PADRE_ALONSO_OVALLE.id, DatosPrueba.UBICACION_CERCA_LA_MONEDA)
        assertNull(control.subida.value)

        //+50: 220, llega al 4
        progreso.registrarVistaCapsula(camila.id, DatosPrueba.PUNTO_LA_MONEDA.id)
        val subida = control.subida.value
        assertNotNull(subida)
        assertEquals(4, subida!!.nivel.numero)
        assertEquals("Guía urbano", subida.nivel.nombre)
        assertEquals(TemaApp.NOCHE_MUSEO, subida.tema)
        assertEquals(camila.username, subida.nombreUsuario)
        //RN-33: queda guardado y con una notificación LOGRO
        assertEquals(4, preferenciasActuales().nivelMaximoAlcanzado)
        assertEquals(TipoNotificacion.LOGRO, notificaciones.lista.single().tipo)
        assertEquals("¡Subiste al nivel 4!", notificaciones.lista.single().titulo)
    }

    @Test
    fun p25SoloLaPrimeraVez() = runBlocking {
        entrar(camila, nivelMaximo = 3)
        progreso.registrarVistaCapsula(camila.id, DatosPrueba.PUNTO_LA_MONEDA.id)
        control.subidaVista()

        //otra suma dentro del mismo nivel no lo repite
        progreso.registrarVistaCapsula(camila.id, DatosPrueba.PUNTO_CATEDRAL.id)
        assertNull(control.subida.value)
        assertEquals(1, notificaciones.lista.size)
    }

    @Test
    fun siBajoYVuelveASubirNoSeRepite() = runBlocking {
        //RN-34: Camila ya había llegado al 5 y ahora tiene 160 (nivel 3); al volver al 4 no hay P25
        entrar(camila, nivelMaximo = 5)
        progreso.registrarVistaCapsula(camila.id, DatosPrueba.PUNTO_LA_MONEDA.id)

        assertNull(control.subida.value)
        assertEquals(5, preferenciasActuales().nivelMaximoAlcanzado)
        assertTrue(notificaciones.lista.isEmpty())
    }

    @Test
    fun siSubeDosNivelesMuestraElMasAlto() = runBlocking {
        //0 puntos y nivel máximo 1; con 120 puntos de golpe pasa al 3
        val nueva = DatosPrueba.USUARIO_NUEVA
        val filas = listOf(DatosPrueba.PUNTO_LA_MONEDA, DatosPrueba.PUNTO_CATEDRAL).map { punto ->
            ProgresoPunto(usuarioId = nueva.id, puntoId = punto.id, escaneoUbicacion = punto.ubicacion, capsulaVistaEn = 1L, puntosCapsula = 50)
        }
        entrar(nueva, nivelMaximo = 1, filas = filas)

        control.revisar(nueva.id)

        assertEquals(3, control.subida.value!!.nivel.numero)
        assertEquals(TemaApp.ATARDECER_MAPOCHO, control.subida.value!!.tema)
        assertEquals(3, preferenciasActuales().nivelMaximoAlcanzado)
        assertEquals(1, notificaciones.lista.size)
    }

    @Test
    fun elAdministradorNoSubeDeNivel() = runBlocking {
        val admin = DatosPrueba.USUARIO_ADMIN
        val filas = listOf(ProgresoPunto(usuarioId = admin.id, puntoId = DatosPrueba.PUNTO_LA_MONEDA.id,
            escaneoUbicacion = DatosPrueba.UBICACION_CERCA_LA_MONEDA, capsulaVistaEn = 1L, puntosCapsula = 50))
        entrar(admin, nivelMaximo = 1, filas = filas)

        control.revisar(admin.id)

        assertNull(control.subida.value)
        assertTrue(notificaciones.lista.isEmpty())
    }

    @Test
    fun usarEsteTemaLoAplica() = runBlocking {
        entrar(camila, nivelMaximo = 3)
        progreso.registrarVistaCapsula(camila.id, DatosPrueba.PUNTO_LA_MONEDA.id)
        val dialogo = SubidaNivelViewModel(control, preferencias)

        dialogo.onEvent(SubidaNivelUiEvent.OnUsarTema)

        assertNull(dialogo.subida.value)
        assertEquals(TemaApp.NOCHE_MUSEO, preferenciasActuales().tema)
        //no pisa el nivel recién guardado
        assertEquals(4, preferenciasActuales().nivelMaximoAlcanzado)
    }

    @Test
    fun seguirExplorandoNoCambiaElTema() = runBlocking {
        entrar(camila, nivelMaximo = 3)
        progreso.registrarVistaCapsula(camila.id, DatosPrueba.PUNTO_LA_MONEDA.id)
        val dialogo = SubidaNivelViewModel(control, preferencias)

        dialogo.onEvent(SubidaNivelUiEvent.OnSeguirExplorando)

        assertNull(dialogo.subida.value)
        assertEquals(TemaApp.CLASICO, preferenciasActuales().tema)
    }

    @Test
    fun aparienciaSoloDejaElegirTemasDesbloqueados() {
        entrar(camila, nivelMaximo = 3)
        val configuracion = ConfiguracionViewModel(preferencias, usuarios, base, FakeCapsulaRepository())

        assertEquals(TemaApp.entries.take(3).toSet(), configuracion.estado.value.temasDesbloqueados)
        assertEquals(3, configuracion.estado.value.nivel!!.numero)

        //LVL-03: el bloqueado no responde
        configuracion.onEvent(ConfiguracionUiEvent.OnTemaChange(TemaApp.IA_NEON))
        assertEquals(TemaApp.CLASICO, configuracion.estado.value.tema)

        configuracion.onEvent(ConfiguracionUiEvent.OnTemaChange(TemaApp.ATARDECER_MAPOCHO))
        assertEquals(TemaApp.ATARDECER_MAPOCHO, configuracion.estado.value.tema)
        assertEquals(TemaApp.ATARDECER_MAPOCHO, preferenciasActuales().tema)
    }

    @Test
    fun elAdministradorTieneLosOchoTemasYNoTieneNivel() {
        entrar(DatosPrueba.USUARIO_ADMIN, nivelMaximo = 1)
        val configuracion = ConfiguracionViewModel(preferencias, usuarios, base, FakeCapsulaRepository())

        assertEquals(TemaApp.entries.toSet(), configuracion.estado.value.temasDesbloqueados)
        assertNull(configuracion.estado.value.nivel)

        configuracion.onEvent(ConfiguracionUiEvent.OnTemaChange(TemaApp.IA_NEON))
        assertEquals(TemaApp.IA_NEON, preferenciasActuales().tema)
    }
}
