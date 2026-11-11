package com.cookingplastic.triskeledu.ui.screens.ranking

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.ui.screens.mapa.PuntoMapa
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

//LUG-06 (P20) y MAP-03 (enRango)
@OptIn(ExperimentalCoroutinesApi::class)
class RankingTest {

    private val zona = ZoneId.of("America/Santiago")
    //5 de noviembre de 2026 a las 12:00
    private val ahora = LocalDateTime.of(2026, 11, 5, 12, 0).atZone(zona).toInstant().toEpochMilli()
    private val octubre = LocalDateTime.of(2026, 10, 20, 12, 0).atZone(zona).toInstant().toEpochMilli()
    private val noviembre = LocalDateTime.of(2026, 11, 2, 12, 0).atZone(zona).toInstant().toEpochMilli()

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun ordenSiempreConElUsuarioActualAparte() {
        val (primeros, actual) = armarRanking(
            DatosPrueba.usuarios, DatosPrueba.progreso, DatosPrueba.USUARIO_EXPLORADOR.id, PeriodoRanking.SIEMPRE, ahora, zona
        )
        assertEquals(listOf("maestro_demo", "sabio_demo", "guardian_demo", "contenido_ia", "tomas_ruta"), primeros.map { it.username })
        assertEquals(listOf(1, 2, 3, 4, 5), primeros.map { it.posicion })
        assertEquals(listOf(1040, 800, 560, 400, 220), primeros.map { it.puntos })
        //explorador_demo está séptimo: se muestra aparte y destacado
        assertEquals("explorador_demo", actual?.username)
        assertEquals(7, actual?.posicion)
        assertEquals(70, actual?.puntos)
        assertTrue(actual!!.esActual)
        assertTrue(primeros.none { it.esActual })
    }

    @Test
    fun elAdministradorYLasCuentasEnRevisionNoAparecen() {
        val (primeros, _) = armarRanking(DatosPrueba.usuarios, DatosPrueba.progreso, null, PeriodoRanking.SIEMPRE, ahora, zona, cantidad = 50)
        val nombres = primeros.map { it.username }
        assertFalse("admin_triskel" in nombres)
        assertFalse("pendiente_demo" in nombres)
        assertEquals(8, primeros.size)
    }

    @Test
    fun siEstaEntreLosPrimerosSeDestacaAhiMismo() {
        val (primeros, actual) = armarRanking(
            DatosPrueba.usuarios, DatosPrueba.progreso, DatosPrueba.USUARIO_SABIO.id, PeriodoRanking.SIEMPRE, ahora, zona
        )
        assertNull(actual)
        assertTrue(primeros.single { it.esActual }.username == "sabio_demo")
    }

    private fun usuario(id: String) = Usuario(id = id, username = id, correo = "$id@ejemplo.cl", claveHash = "", claveSal = "", terminosVersion = "1.0", terminosAceptadosEn = 0)

    @Test
    fun esteMesCuentaCadaActividadEnElMesQueSeHizo() {
        val ana = usuario("ana")
        val beto = usuario("beto")
        val filas = listOf(
            //Ana escaneó en octubre y vio la cápsula en noviembre: este mes suma solo los 50
            ProgresoPunto(usuarioId = "ana", puntoId = "p1", escaneoUbicacion = DatosPrueba.UBICACION_CERCA_LA_MONEDA,
                escaneadoEn = octubre, capsulaVistaEn = noviembre, puntosCapsula = 50),
            //Beto escaneó dos lugares en octubre y uno en noviembre
            ProgresoPunto(usuarioId = "beto", puntoId = "p1", escaneoUbicacion = DatosPrueba.UBICACION_CERCA_LA_MONEDA, escaneadoEn = octubre,
                capsulaVistaEn = octubre, puntosCapsula = 50, selfieEn = octubre, puntosSelfie = 20),
            ProgresoPunto(usuarioId = "beto", puntoId = "p2", escaneoUbicacion = DatosPrueba.UBICACION_CERCA_LA_MONEDA, escaneadoEn = noviembre)
        )
        val (mes, _) = armarRanking(listOf(ana, beto), filas, "beto", PeriodoRanking.ESTE_MES, ahora, zona)
        assertEquals(listOf("ana" to 50, "beto" to 10), mes.map { it.username to it.puntos })

        val (siempre, _) = armarRanking(listOf(ana, beto), filas, "beto", PeriodoRanking.SIEMPRE, ahora, zona)
        assertEquals(listOf("beto" to 90, "ana" to 60), siempre.map { it.username to it.puntos })
    }

    @Test
    fun conEmpateCompartenPosicion() {
        val usuarios = listOf(usuario("carla"), usuario("bruno"), usuario("abel"))
        val filas = listOf(
            ProgresoPunto(usuarioId = "carla", puntoId = "p1", escaneoUbicacion = DatosPrueba.UBICACION_CERCA_LA_MONEDA, escaneadoEn = noviembre),
            ProgresoPunto(usuarioId = "bruno", puntoId = "p1", escaneoUbicacion = DatosPrueba.UBICACION_CERCA_LA_MONEDA, escaneadoEn = noviembre)
        )
        val (primeros, _) = armarRanking(usuarios, filas, null, PeriodoRanking.SIEMPRE, ahora, zona)
        //empatados se ordenan por nombre
        assertEquals(listOf("bruno" to 1, "carla" to 1, "abel" to 3), primeros.map { it.username to it.posicion })
    }

    @Test
    fun elViewModelCambiaDePeriodoYSigueLosPuntos() = runBlocking {
        val usuarios = FakeUsuarioRepository()
        usuarios.iniciarSesion(DatosPrueba.USUARIO_CAMILA.correo, DatosPrueba.CLAVE_DE_PRUEBA)
        val progreso = FakeProgresoRepository(DatosPrueba.progreso)
        val viewModel = RankingViewModel(progreso, usuarios, ahora = { ahora }, zona = zona)

        assertEquals(PeriodoRanking.ESTE_MES, viewModel.estado.value.periodo)
        viewModel.onEvent(RankingUiEvent.OnPeriodoChange(PeriodoRanking.SIEMPRE))
        val estado = viewModel.estado.value
        assertFalse(estado.cargando)
        assertEquals(PeriodoRanking.SIEMPRE, estado.periodo)
        assertEquals("camila_explora", estado.filaActual?.username)
        assertEquals(6, estado.filaActual?.posicion)

        //Camila escanea un lugar nuevo y el ranking se actualiza solo
        progreso.registrarEscaneo(DatosPrueba.USUARIO_CAMILA.id, DatosPrueba.PUNTO_SANTA_LUCIA.id, DatosPrueba.PUNTO_SANTA_LUCIA.ubicacion)
        assertEquals(170, viewModel.estado.value.filaActual?.puntos)
    }

    //---------- MAP-03 ----------

    @Test
    fun enRangoSoloBloqueadoActivoA100MetrosOMenos() {
        val punto = DatosPrueba.PUNTO_LA_MONEDA
        assertTrue(PuntoMapa(punto, EstadoPunto.BLOQUEADO, 99.0).enRango)
        assertTrue(PuntoMapa(punto, EstadoPunto.BLOQUEADO, 100.0).enRango)
        //al salir del radio deja de latir
        assertFalse(PuntoMapa(punto, EstadoPunto.BLOQUEADO, 100.5).enRango)
        //sin ubicación no hay distancia
        assertFalse(PuntoMapa(punto, EstadoPunto.BLOQUEADO, null).enRango)
        //ya escaneado o completado no late
        assertFalse(PuntoMapa(punto, EstadoPunto.DESBLOQUEADO, 10.0).enRango)
        assertFalse(PuntoMapa(punto, EstadoPunto.COMPLETADO, 10.0).enRango)
        //un punto inactivo o sin cápsula no se puede escanear
        assertFalse(PuntoMapa(punto, EstadoPunto.BLOQUEADO, 10.0, EstadoAdminPunto.INACTIVO).enRango)
        assertFalse(PuntoMapa(punto, EstadoPunto.BLOQUEADO, 10.0, EstadoAdminPunto.SIN_CAPSULA).enRango)
    }
}
