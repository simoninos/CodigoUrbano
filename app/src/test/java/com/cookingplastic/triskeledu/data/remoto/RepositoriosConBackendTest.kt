package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.data.creador.GestorCapsulas
import com.cookingplastic.triskeledu.data.creador.ResultadoCapsula
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.CategoriaCapsula
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Sesion
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.repository.FakeCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.ConnectException

//Los repositorios ConBackend: primero el servidor (simulado con MockK), después Room (los Fake en memoria)
class RepositoriosConBackendTest {

    private val api = mockk<BackendApi>()

    // ---------- UsuariosConBackend (BKD-02) ----------

    private fun usuarioDto(id: String = DatosPrueba.USUARIO_CAMILA.id, solicitud: EstadoSolicitud? = null) = UsuarioDto(
        id = id, username = "camila_explora", correo = "camila.explora@ejemplo.cl", rol = RolUsuario.CLIENTE,
        terminosVersion = "1.1", terminosAceptadosEn = 1, creadoEn = 2, solicitudCreador = solicitud,
        solicitadoEn = null, solicitudResueltaEn = null
    )

    @Test
    fun login_guardaLaCopiaDelUsuario_yLaSesionConSuToken() = runTest {
        val dao = mockk<UsuarioDao>(relaxed = true)
        coEvery { api.iniciarSesion(any()) } returns SesionDto("token-1", 1, false, usuarioDto())
        val repo = UsuariosConBackend(FakeUsuarioRepository(), dao, api)

        val usuario = repo.iniciarSesion("camila.explora@ejemplo.cl", "Demo1234")

        assertEquals(DatosPrueba.USUARIO_CAMILA.id, usuario?.id)
        coVerify { dao.guardarUsuario(match { it.id == DatosPrueba.USUARIO_CAMILA.id && it.claveHash.isNotBlank() }) }
        coVerify { dao.guardarSesion(Sesion(usuarioId = DatosPrueba.USUARIO_CAMILA.id, token = "token-1")) }
    }

    @Test
    fun login_401_esClaveIncorrecta() = runTest {
        coEvery { api.iniciarSesion(any()) } throws ErrorBackend(401, "Correo o contraseña incorrectos")
        val repo = UsuariosConBackend(FakeUsuarioRepository(), mockk(relaxed = true), api)

        assertNull(repo.iniciarSesion("camila.explora@ejemplo.cl", "mala"))
    }

    @Test
    fun login_cuentaEnRevision_noGuardaSesion_RN29() = runTest {
        val dao = mockk<UsuarioDao>(relaxed = true)
        coEvery { api.iniciarSesion(any()) } returns SesionDto(null, null, true, usuarioDto(solicitud = EstadoSolicitud.PENDIENTE))
        val repo = UsuariosConBackend(FakeUsuarioRepository(), dao, api)

        val usuario = repo.iniciarSesion("pendiente@ejemplo.cl", "Demo1234")

        assertEquals(EstadoSolicitud.PENDIENTE, usuario?.solicitudCreador)
        coVerify(exactly = 0) { dao.guardarSesion(any()) }
    }

    @Test
    fun login_sinServidor_entraConLaCopiaDelTelefono_EXC06() = runTest {
        coEvery { api.iniciarSesion(any()) } throws ConnectException("apagado")
        val repo = UsuariosConBackend(FakeUsuarioRepository(), mockk(relaxed = true), api)

        val usuario = repo.iniciarSesion(DatosPrueba.USUARIO_CAMILA.correo, DatosPrueba.CLAVE_DE_PRUEBA)

        assertEquals(DatosPrueba.USUARIO_CAMILA.id, usuario?.id)
    }

    @Test
    fun registro_enviaElIdDelTelefono_yLaCuentaNoSeCreaSiElServidorDice409() = runTest {
        val enviado = slot<RegistroDto>()
        coEvery { api.registrar(capture(enviado)) } throws ErrorBackend(409, "Ese nombre ya está en uso", mapOf("username" to "Ese nombre ya está en uso"))
        val dao = mockk<UsuarioDao>(relaxed = true)
        val repo = UsuariosConBackend(FakeUsuarioRepository(), dao, api, nuevoId = { "id-del-telefono" })

        try {
            repo.registrar("camila_explora", "otra@ejemplo.cl", "Demo1234", "1.1", quiereSerCreador = true)
            fail("debía lanzar ErrorBackend")
        } catch (e: ErrorBackend) {
            assertEquals("Ese nombre ya está en uso", e.campos["username"])
        }
        assertEquals("id-del-telefono", enviado.captured.id)
        assertTrue(enviado.captured.quiereSerCreador)
        coVerify(exactly = 0) { dao.guardarUsuario(any()) }
    }

    @Test
    fun registro_aceptado_guardaLaCopiaConElRolDelServidor() = runTest {
        val dao = mockk<UsuarioDao>(relaxed = true)
        coEvery { api.registrar(any()) } returns SesionDto("t", 1, false, usuarioDto(id = "nuevo"))
        val repo = UsuariosConBackend(FakeUsuarioRepository(), dao, api)

        val usuario = repo.registrar("camila_explora", "camila.explora@ejemplo.cl", "Demo1234", "1.1")

        assertEquals("nuevo", usuario.id)
        assertEquals(RolUsuario.CLIENTE, usuario.rol)
        coVerify { dao.guardarUsuario(match { it.id == "nuevo" }) }
    }

    // ---------- PuntosConBackend (BKD-03) ----------

    private fun puntoNuevo() = PuntoInteres(
        id = "p-nuevo", nombre = "Punto nuevo", descripcion = null, tipo = TipoPuntoInteres.AIPC,
        ubicacion = Ubicacion(-33.4, -70.6, 500.0), imagenUri = "android.resource://x/drawable/y",
        descubreMasUrl = null, codigoQr = "", capsulaId = null
    )

    private fun PuntoInteres.comoDto(activo: Boolean = this.activo, codigoQr: String = this.codigoQr) = PuntoDto(
        id, nombre, descripcion, tipo, ubicacion.latitud, ubicacion.longitud, ubicacion.altitud, imagenUri,
        descubreMasUrl, codigoQr, capsulaId, activo, actualizadoEn
    )

    @Test
    fun puntoNuevo_seCreaEnElServidor_yRoomGuardaElQrQueDevolvio() = runTest {
        val local = FakePuntoInteresRepository()
        coEvery { api.crearPunto(any()) } answers { firstArg<PuntoEnvioDto>().let { puntoNuevo().comoDto(codigoQr = "TRISKEL-PI-017") } }
        val repo = PuntosConBackend(local, api)

        repo.guardar(puntoNuevo())

        coVerify { api.crearPunto(match { it.id == "p-nuevo" && it.codigoQr == null }) }
        assertEquals("TRISKEL-PI-017", local.obtenerPorId("p-nuevo").first()?.codigoQr)
    }

    @Test
    fun editarDatos_usaPut_yActivar_usaSuEndpoint() = runTest {
        val local = FakePuntoInteresRepository()
        val moneda = DatosPrueba.PUNTO_LA_MONEDA
        val editado = moneda.copy(nombre = "Palacio de La Moneda (editado)", activo = !moneda.activo)
        coEvery { api.editarPunto(moneda.id, any()) } returns editado.comoDto(activo = moneda.activo)
        coEvery { api.cambiarActivo(moneda.id, ActivoDto(editado.activo)) } returns editado.comoDto()
        val repo = PuntosConBackend(local, api)

        repo.guardar(editado)

        coVerify { api.editarPunto(moneda.id, match { it.nombre == "Palacio de La Moneda (editado)" }) }
        coVerify { api.cambiarActivo(moneda.id, ActivoDto(editado.activo)) }
        assertEquals("Palacio de La Moneda (editado)", local.obtenerPorId(moneda.id).first()?.nombre)
    }

    @Test
    fun soloCambiarLaCapsula_usaElEndpointDelCreador_CRE08() = runTest {
        val sinCapsula = DatosPrueba.puntos.first { it.capsulaId == null }
        val local = FakePuntoInteresRepository()
        coEvery { api.asociarCapsula(sinCapsula.id, AsociarDto("cap-1")) } returns sinCapsula.copy(capsulaId = "cap-1").comoDto()
        val repo = PuntosConBackend(local, api)

        repo.guardar(sinCapsula.copy(capsulaId = "cap-1"))

        coVerify(exactly = 0) { api.editarPunto(any(), any()) }
        coVerify { api.asociarCapsula(sinCapsula.id, AsociarDto("cap-1")) }
    }

    @Test
    fun siElServidorRechazaEliminar_elPuntoSigueEnElTelefono_RN30() = runTest {
        val local = FakePuntoInteresRepository()
        coEvery { api.eliminarPunto(any()) } throws ErrorBackend(409, "Ya hay usuarios con progreso aquí")
        val repo = PuntosConBackend(local, api)

        try {
            repo.eliminar(DatosPrueba.PUNTO_LA_MONEDA.id)
            fail("debía lanzar ErrorBackend")
        } catch (e: ErrorBackend) {
            assertEquals(409, e.codigo)
        }
        assertNotNull(local.obtenerPorId(DatosPrueba.PUNTO_LA_MONEDA.id).first())
    }

    @Test
    fun cambiarActivo_guardaLoQueRespondioElServidor() = runTest {
        val local = FakePuntoInteresRepository()
        val moneda = DatosPrueba.PUNTO_LA_MONEDA
        coEvery { api.cambiarActivo(moneda.id, ActivoDto(false)) } returns moneda.comoDto(activo = false)

        assertTrue(PuntosConBackend(local, api).cambiarActivo(moneda.id, false))
        assertFalse(local.obtenerPorId(moneda.id).first()!!.activo)
    }

    // ---------- CapsulasConBackend y GestorCapsulas (CRE-02 a CRE-06) ----------

    private val capsula = Capsula(
        id = "cap-nueva", titulo = "Qué es la IA", urlStream = "https://drive.google.com/file/d/abc/view",
        categoria = CategoriaCapsula.QUE_ES_IA, duracionSegundos = 120, creadorId = DatosPrueba.USUARIO_CONTENIDO.id
    )

    private fun Capsula.comoDto() = CapsulaDto(id, titulo, descripcion, urlStream, categoria, duracionSegundos, estado, creadorId, creadaEn, publicadaEn)

    @Test
    fun capsulaNueva_seCrea_yPublicarUsaSuEndpoint() = runTest {
        val local = FakeCapsulaRepository()
        coEvery { api.crearCapsula(any()) } returns capsula.comoDto()
        coEvery { api.publicarCapsula("cap-nueva") } returns capsula.copy(estado = EstadoCapsula.PUBLICADA).comoDto()
        val repo = CapsulasConBackend(local, api)

        repo.guardar(capsula)
        repo.guardar(capsula.copy(estado = EstadoCapsula.PUBLICADA))

        coVerify(exactly = 1) { api.crearCapsula(any()) }
        coVerify(exactly = 0) { api.editarCapsula(any(), any()) }
        coVerify { api.publicarCapsula("cap-nueva") }
        assertEquals(EstadoCapsula.PUBLICADA, local.obtenerPorId("cap-nueva")?.estado)
    }

    @Test
    fun editarYRetirar_usanSusEndpoints_yEliminarBorraDelTelefono() = runTest {
        val publicada = capsula.copy(estado = EstadoCapsula.PUBLICADA)
        val local = FakeCapsulaRepository(listOf(publicada))
        coEvery { api.retirarCapsula("cap-nueva") } returns capsula.comoDto()
        coEvery { api.editarCapsula("cap-nueva", any()) } returns capsula.comoDto()
        coEvery { api.eliminarCapsula("cap-nueva") } returns retrofit2.Response.success(204, Unit)
        val repo = CapsulasConBackend(local, api)

        repo.guardar(publicada.copy(estado = EstadoCapsula.BORRADOR))
        repo.guardar(capsula.copy(titulo = "Otro título"))
        repo.eliminar("cap-nueva")

        coVerify { api.retirarCapsula("cap-nueva") }
        coVerify { api.editarCapsula("cap-nueva", match { it.titulo == "Otro título" }) }
        assertNull(local.obtenerPorId("cap-nueva"))
    }

    @Test
    fun gestor_conElServidorCaido_devuelveElMensaje_yNoCambiaNada() = runTest {
        val local = FakeCapsulaRepository()
        coEvery { api.crearCapsula(any()) } throws ConnectException("apagado")
        val gestor = GestorCapsulas(CapsulasConBackend(local, api), FakePuntoInteresRepository())

        val resultado = gestor.guardar(capsula)

        assertEquals(ResultadoCapsula.Error("No hay conexión con el servidor. Intenta más tarde"), resultado)
        assertNull(local.obtenerPorId("cap-nueva"))
    }

    // ---------- ActualizadorContenido ----------

    @Test
    fun actualizar_traeCapsulasYPuntos_aRoom_ySaltaLasFilasQueNoCalzan() = runTest {
        val capsulas = FakeCapsulaRepository(emptyList())
        val puntos = FakePuntoInteresRepository(emptyList())
        val moneda = DatosPrueba.PUNTO_LA_MONEDA
        coEvery { api.capsulas() } returns listOf(capsula.comoDto())
        coEvery { api.puntos() } returns listOf(moneda.comoDto(), puntoNuevo().comoDto())
        val puntosQueFallan = object : com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository by puntos {
            override suspend fun guardar(punto: PuntoInteres) {
                if (punto.id == "p-nuevo") error("clave foránea") else puntos.guardar(punto)
            }
        }

        val resumen = ActualizadorContenido(api, capsulas, puntosQueFallan).actualizar()

        assertEquals(ResumenActualizacion(capsulas = 1, puntos = 1), resumen)
        assertNotNull(puntos.obtenerPorId(moneda.id).first())
        assertNotNull(capsulas.obtenerPorId("cap-nueva"))
    }
}
