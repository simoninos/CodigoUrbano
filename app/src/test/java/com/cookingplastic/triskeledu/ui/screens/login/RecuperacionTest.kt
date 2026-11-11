package com.cookingplastic.triskeledu.ui.screens.login

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.local.dao.CodigoRecuperacionDao
import com.cookingplastic.triskeledu.data.local.dao.NotificacionDao
import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.CodigoRecuperacion
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Sesion
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.notificaciones.AvisoLocal
import com.cookingplastic.triskeledu.data.security.HashContrasena
import com.cookingplastic.triskeledu.data.simulacion.RecuperacionSimulada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

//SIM-01 (ACC-05): código de 6 dígitos válido, vencido, ya usado o distinto
@OptIn(ExperimentalCoroutinesApi::class)
class RecuperacionTest {

    //DAOs en memoria: lo justo para la simulación
    private class UsuariosEnMemoria : UsuarioDao {
        val lista = DatosPrueba.usuarios.toMutableList()
        override suspend fun insertar(usuario: Usuario) { lista += usuario }
        override suspend fun buscarPorCorreo(correo: String) = lista.find { it.correo == correo }
        override suspend fun existeUsername(username: String) = lista.any { it.username == username }
        override suspend fun existeCorreo(correo: String) = lista.any { it.correo == correo }
        override suspend fun cambiarClave(id: String, hash: String, sal: String) {
            lista.replaceAll { if (it.id == id) it.copy(claveHash = hash, claveSal = sal) else it }
        }
        override suspend fun guardarSesion(sesion: Sesion) = Unit
        override fun usuarioActual(): Flow<Usuario?> = flowOf(null)
        override suspend fun buscarPorId(id: String) = lista.find { it.id == id }
        override fun solicitudesPendientes(): Flow<List<Usuario>> = flowOf(emptyList())
        override suspend fun resolverSolicitud(id: String, rol: RolUsuario, estado: EstadoSolicitud, fecha: Long, adminId: String) = 0
        override suspend fun todos(): List<Usuario> = lista
        override suspend fun cambiarUsername(id: String, username: String) = Unit
    }

    private class CodigosEnMemoria : CodigoRecuperacionDao {
        val mapa = mutableMapOf<String, CodigoRecuperacion>()
        override suspend fun guardar(codigo: CodigoRecuperacion) { mapa[codigo.usuarioId] = codigo }
        override suspend fun buscar(usuarioId: String) = mapa[usuarioId]
        override suspend fun marcarUsado(usuarioId: String) { mapa[usuarioId]?.let { mapa[usuarioId] = it.copy(usado = true) } }
    }

    private class NotificacionesEnMemoria : NotificacionDao {
        val lista = mutableListOf<Notificacion>()
        override fun notificacionesDe(usuarioId: String) = MutableStateFlow(lista.filter { it.usuarioId == usuarioId })
        override fun sinLeer(usuarioId: String) = MutableStateFlow(lista.count { it.usuarioId == usuarioId && !it.leida })
        override suspend fun insertar(notificacion: Notificacion) { lista += notificacion }
        override suspend fun marcarLeida(id: String) = Unit
        override suspend fun marcarTodasLeidas(usuarioId: String) = Unit
    }

    private class AvisoAnotado : AvisoLocal {
        val avisos = mutableListOf<String>()
        override fun mostrar(tipo: TipoNotificacion, titulo: String, texto: String) { avisos += texto }
    }

    private val usuarios = UsuariosEnMemoria()
    private val codigos = CodigosEnMemoria()
    private val notificaciones = NotificacionesEnMemoria()
    private val aviso = AvisoAnotado()
    private var reloj = 1_000_000L
    private val recuperacion = RecuperacionSimulada(
        usuarios, codigos, notificaciones, aviso,
        ahora = { reloj },
        generarCodigo = { "481726" },
        calculo = Dispatchers.Unconfined
    )
    private val camila = DatosPrueba.USUARIO_CAMILA.correo

    @Before
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun despues() = Dispatchers.resetMain()

    @Test
    fun codigoValidoCambiaLaClaveYLlegaComoNotificacion() = runBlocking {
        recuperacion.enviarCodigo("Camila.Explora@Ejemplo.cl")

        //el código llega como notificación SISTEMA y queda en el historial; en la tabla solo va su hash
        assertTrue(aviso.avisos.single().contains("481726"))
        assertEquals(TipoNotificacion.SISTEMA, notificaciones.lista.single().tipo)
        val guardado = codigos.mapa.getValue(DatosPrueba.USUARIO_CAMILA.id)
        assertFalse(guardado.codigoHash.contains("481726"))
        assertEquals(reloj + 15 * 60_000L, guardado.venceEn)

        assertTrue(recuperacion.cambiarClave(camila, "481726", "Nueva1234"))
        val actualizada = usuarios.buscarPorCorreo(camila)!!
        assertTrue(HashContrasena.verificar("Nueva1234", actualizada.claveHash, actualizada.claveSal))
        assertFalse(HashContrasena.verificar("Demo1234", actualizada.claveHash, actualizada.claveSal))
    }

    @Test
    fun codigoVencido() = runBlocking {
        recuperacion.enviarCodigo(camila)
        //pasan 15 minutos y un segundo
        reloj += 15 * 60_000L + 1_000L

        assertFalse(recuperacion.cambiarClave(camila, "481726", "Nueva1234"))
    }

    @Test
    fun codigoYaUsado() = runBlocking {
        recuperacion.enviarCodigo(camila)
        assertTrue(recuperacion.cambiarClave(camila, "481726", "Nueva1234"))

        //sirve una sola vez
        assertFalse(recuperacion.cambiarClave(camila, "481726", "Otra5678"))
    }

    @Test
    fun codigoDistinto() = runBlocking {
        recuperacion.enviarCodigo(camila)

        assertFalse(recuperacion.cambiarClave(camila, "000000", "Nueva1234"))
    }

    @Test
    fun correoQueNoExisteNoEnviaNada() = runBlocking {
        recuperacion.enviarCodigo("nadie@ejemplo.cl")

        assertTrue(aviso.avisos.isEmpty())
        assertTrue(codigos.mapa.isEmpty())
    }

    @Test
    fun pantallaConCodigoMalo() {
        val vm = RecuperarViewModel(recuperacion)
        vm.onEvent(RecuperarUiEvent.OnCorreoChange(camila))
        vm.onEvent(RecuperarUiEvent.OnEnviarCodigo)
        //la pantalla pide el permiso y avisa la respuesta
        assertTrue(vm.estado.value.pedirPermiso)
        vm.onEvent(RecuperarUiEvent.OnPermisoRespondido(concedido = true))
        assertTrue(vm.estado.value.codigoEnviado)

        //RN-18: con todo vacío marca los tres campos y enfoca el código
        vm.onEvent(RecuperarUiEvent.OnCambiarClave)
        assertEquals(CampoRecuperar.CODIGO, vm.estado.value.campoConError)
        assertEquals("Repite tu contraseña", vm.estado.value.errorRepetirClave)

        vm.onEvent(RecuperarUiEvent.OnCodigoChange("12a3456"))
        assertEquals("123456", vm.estado.value.codigo)
        vm.onEvent(RecuperarUiEvent.OnClaveNuevaChange("Nueva1234"))
        vm.onEvent(RecuperarUiEvent.OnRepetirClaveChange("Nueva1234"))
        vm.onEvent(RecuperarUiEvent.OnCambiarClave)
        assertEquals("El código no es válido o ya venció", vm.estado.value.errorCodigo)
        assertFalse(vm.estado.value.listo)

        vm.onEvent(RecuperarUiEvent.OnCodigoChange("481726"))
        vm.onEvent(RecuperarUiEvent.OnCambiarClave)
        assertTrue(vm.estado.value.listo)
    }
}
