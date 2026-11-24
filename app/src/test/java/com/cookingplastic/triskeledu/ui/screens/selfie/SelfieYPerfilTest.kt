package com.cookingplastic.triskeledu.ui.screens.selfie

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.local.dao.NotificacionDao
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.nivel.ControlNivel
import com.cookingplastic.triskeledu.data.nivel.ProgresoConNivel
import com.cookingplastic.triskeledu.data.notificaciones.AvisoLocal
import com.cookingplastic.triskeledu.data.notificaciones.GeneradorNotificaciones
import com.cookingplastic.triskeledu.data.repository.FakeNotificacionRepository
import com.cookingplastic.triskeledu.data.repository.FakePreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.ui.screens.perfil.PerfilUiEvent
import com.cookingplastic.triskeledu.ui.screens.perfil.CampoPerfil
import com.cookingplastic.triskeledu.ui.screens.perfil.PerfilViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

//ACT-03 (selfie) y PER-02 (perfil)
@OptIn(ExperimentalCoroutinesApi::class)
class SelfieYPerfilTest {

    private val camila = DatosPrueba.USUARIO_CAMILA
    //Bellas Artes: Camila ya vio la cápsula y le falta la selfie
    private val bellasArtes = DatosPrueba.PUNTO_BELLAS_ARTES
    private lateinit var usuarios: FakeUsuarioRepository
    private lateinit var progreso: FakeProgresoRepository

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        usuarios = FakeUsuarioRepository()
        progreso = FakeProgresoRepository(DatosPrueba.progreso)
        runBlocking { usuarios.iniciarSesion(camila.correo, DatosPrueba.CLAVE_DE_PRUEBA) }
    }

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun laSelfieSuma20UnaSolaVez() = runBlocking {
        val antes = progreso.totalPuntos(camila.id).first()

        assertTrue(progreso.registrarSelfie(camila.id, bellasArtes.id, "file:///selfie1.jpg"))
        assertFalse(progreso.registrarSelfie(camila.id, bellasArtes.id, "file:///selfie2.jpg"))

        assertEquals(antes + 20, progreso.totalPuntos(camila.id).first())
        val fila = progreso.progresoDePunto(camila.id, bellasArtes.id).first()!!
        assertEquals("file:///selfie1.jpg", fila.selfieUri)
        assertNotNull(fila.selfieEn)
        //sin escanear no se puede (RN-07)
        assertFalse(progreso.registrarSelfie(camila.id, DatosPrueba.PUNTO_PADRE_ALONSO_OVALLE.id, "file:///x.jpg"))
    }

    @Test
    fun laPantallaDeSelfieRegistraYNoRepite() {
        val pantalla = SelfieViewModel(bellasArtes.id, FakePuntoInteresRepository(), usuarios, progreso)
        assertTrue(pantalla.estado.value.desbloqueado)
        assertFalse(pantalla.estado.value.yaTomada)

        pantalla.onEvent(SelfieUiEvent.OnFotoGuardada("file:///selfie.jpg"))
        assertEquals(20, pantalla.estado.value.puntosGanados)

        //otra vez en el mismo lugar: ya está hecha
        val otra = SelfieViewModel(bellasArtes.id, FakePuntoInteresRepository(), usuarios, progreso)
        assertTrue(otra.estado.value.yaTomada)
        otra.onEvent(SelfieUiEvent.OnFotoGuardada("file:///otra.jpg"))
        assertNull(otra.estado.value.puntosGanados)
        assertEquals("Ya subiste tu selfie en este lugar", otra.estado.value.mensajeError)
    }

    @Test
    fun conLaSelfieElLugarQuedaCompletoYLlegaElLogro() = runBlocking {
        val notificaciones = FakeNotificacionRepository()
        val avisos = mutableListOf<String>()
        val generador = GeneradorNotificaciones(
            notificaciones,
            object : AvisoLocal { override fun mostrar(tipo: TipoNotificacion, titulo: String, texto: String) { avisos += titulo } },
            usuarios,
            FakePreferenciasRepository()
        )
        val puntos = FakePuntoInteresRepository()
        val control = ControlNivel(progreso, FakePreferenciasRepository(), usuarios, SinNotificaciones())
        val conNivel = ProgresoConNivel(progreso, control) { usuarioId, puntoId ->
            puntos.obtenerPorId(puntoId).first()?.let { generador.lugarCompletado(usuarioId, it) }
        }

        conNivel.registrarSelfie(camila.id, bellasArtes.id, "file:///selfie.jpg")

        val logro = notificaciones.todas.single()
        assertEquals(TipoNotificacion.LOGRO, logro.tipo)
        assertEquals("¡Lugar completado!", logro.titulo)
        assertEquals(bellasArtes.id, logro.puntoId)
        //la recibe el usuario conectado: también aparece en el sistema
        assertEquals(listOf("¡Lugar completado!"), avisos)
    }

    @Test
    fun perfilAvisaNombreEnUso() {
        val perfil = PerfilViewModel(usuarios, progreso)
        assertEquals("camila_explora", perfil.estado.value.username)
        assertEquals(3, perfil.estado.value.nivel?.numero)

        perfil.onEvent(PerfilUiEvent.OnUsernameChange("tomas_ruta"))
        perfil.onEvent(PerfilUiEvent.OnGuardar)

        assertEquals(PerfilViewModel.YA_EN_USO, perfil.estado.value.errorUsername)
        assertEquals(CampoPerfil.USERNAME, perfil.estado.value.campoConError)
        assertEquals("camila_explora", runBlocking { usuarios.usuarioActual().first()?.username })
    }

    @Test
    fun perfilAvisaContrasenaActualIncorrecta() {
        val perfil = PerfilViewModel(usuarios, progreso)
        perfil.onEvent(PerfilUiEvent.OnClaveActualChange("otraClave1"))
        perfil.onEvent(PerfilUiEvent.OnClaveNuevaChange("NuevaClave9"))
        perfil.onEvent(PerfilUiEvent.OnRepetirClaveChange("NuevaClave9"))

        perfil.onEvent(PerfilUiEvent.OnGuardar)

        assertEquals(PerfilViewModel.CLAVE_INCORRECTA, perfil.estado.value.errorClaveActual)
        assertEquals(CampoPerfil.CLAVE_ACTUAL, perfil.estado.value.campoConError)
    }

    @Test
    fun perfilValidaComoElRegistro() {
        val perfil = PerfilViewModel(usuarios, progreso)
        perfil.onEvent(PerfilUiEvent.OnUsernameChange("ca"))
        perfil.onEvent(PerfilUiEvent.OnClaveNuevaChange("corta"))
        perfil.onEvent(PerfilUiEvent.OnRepetirClaveChange("distinta"))

        perfil.onEvent(PerfilUiEvent.OnGuardar)

        val estado = perfil.estado.value
        assertEquals("Usa de 3 a 20 caracteres: letras, números o _", estado.errorUsername)
        assertEquals("Ingresa tu contraseña actual", estado.errorClaveActual)
        assertEquals("Mínimo 8 caracteres, con letras y números", estado.errorClaveNueva)
        assertEquals("Las contraseñas no coinciden", estado.errorRepetir)
        assertEquals(CampoPerfil.USERNAME, estado.campoConError)
    }

    @Test
    fun perfilGuardaNombreYContrasena() = runBlocking {
        val perfil = PerfilViewModel(usuarios, progreso)
        perfil.onEvent(PerfilUiEvent.OnUsernameChange("camila_viaja"))
        perfil.onEvent(PerfilUiEvent.OnClaveActualChange(DatosPrueba.CLAVE_DE_PRUEBA))
        perfil.onEvent(PerfilUiEvent.OnClaveNuevaChange("NuevaClave9"))
        perfil.onEvent(PerfilUiEvent.OnRepetirClaveChange("NuevaClave9"))

        perfil.onEvent(PerfilUiEvent.OnGuardar)

        assertEquals("Cambios guardados", perfil.estado.value.mensaje)
        assertEquals("camila_viaja", usuarios.usuarioActual().first()?.username)
        assertEquals("", perfil.estado.value.claveActual)
        //la contraseña nueva sirve y la vieja ya no
        assertNull(usuarios.iniciarSesion(camila.correo, DatosPrueba.CLAVE_DE_PRUEBA))
        assertNotNull(usuarios.iniciarSesion(camila.correo, "NuevaClave9"))
    }

    //ControlNivel necesita un DAO de notificaciones; aquí no se revisa
    private class SinNotificaciones : NotificacionDao {
        override fun notificacionesDe(usuarioId: String) = MutableStateFlow(emptyList<Notificacion>())
        override fun sinLeer(usuarioId: String) = MutableStateFlow(0)
        override suspend fun insertar(notificacion: Notificacion) = Unit
        override suspend fun marcarLeida(id: String) = Unit
        override suspend fun marcarTodasLeidas(usuarioId: String) = Unit
    }
}
