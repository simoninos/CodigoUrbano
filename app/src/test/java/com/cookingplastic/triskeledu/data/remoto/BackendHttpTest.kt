package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.sincronizacion.MotivoRechazo
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

//BKD-01: las llamadas reales de Retrofit contra un servidor HTTP de prueba (MockWebServer):
//qué ruta y qué JSON envía la app, qué cabecera lleva el token y cómo se leen las respuestas y los errores
class BackendHttpTest {

    private lateinit var servidor: MockWebServer
    private var token: String? = null
    private var vencio = false
    private lateinit var api: BackendApi

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        api = ClienteBackend.crear(servidor.url("/").toString(), token = { token }, alVencer = { vencio = true })
    }

    @After
    fun cerrar() {
        runCatching { servidor.shutdown() }
    }

    private fun responder(codigo: Int, cuerpo: String = "") =
        servidor.enqueue(MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo))

    @Test
    fun login_enviaCorreoYClave_yLeeLaSesion() = runTest {
        responder(200, """{"token":"abc.def.ghi","expiraEn":1795043200000,"cuentaEnRevision":false,
            "usuario":{"id":"u1","username":"camila_explora","correo":"camila@ejemplo.cl","rol":"CLIENTE",
            "terminosVersion":"1.1","terminosAceptadosEn":1,"creadoEn":2,"solicitudCreador":null}}""")

        val sesion = llamar { api.iniciarSesion(LoginDto("camila@ejemplo.cl", "Demo1234")) }

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/api/auth/login", peticion.path)
        assertEquals("""{"correo":"camila@ejemplo.cl","clave":"Demo1234"}""", peticion.body.readUtf8())
        //sin sesión todavía: no va el encabezado del token
        assertNull(peticion.getHeader("Authorization"))
        assertEquals("abc.def.ghi", sesion.token)
        assertEquals(RolUsuario.CLIENTE, sesion.usuario.rol)
    }

    @Test
    fun conSesion_cadaPeticionLlevaElToken_BKD05() = runTest {
        token = "token-de-camila"
        responder(200, "[]")

        llamar { api.puntos() }

        assertEquals("Bearer token-de-camila", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun un401ConToken_avisaQueLaSesionVencio_EXC07() = runTest {
        token = "token-vencido"
        responder(401)

        try {
            llamar { api.puntos() }
            fail("debía lanzar ErrorBackend")
        } catch (e: ErrorBackend) {
            assertEquals(401, e.codigo)
        }
        assertTrue(vencio)
    }

    @Test
    fun un401SinToken_esClaveIncorrecta_yNoCierraNada() = runTest {
        responder(401, """{"error":"Correo o contraseña incorrectos","campos":{}}""")

        try {
            llamar { api.iniciarSesion(LoginDto("camila@ejemplo.cl", "mala")) }
            fail("debía lanzar ErrorBackend")
        } catch (e: ErrorBackend) {
            assertEquals("Correo o contraseña incorrectos", e.message)
        }
        assertFalse(vencio)
    }

    @Test
    fun un409_traeElMensajeYElCampo() = runTest {
        responder(409, """{"error":"Ese correo ya está registrado","campos":{"correo":"Ese correo ya está registrado"}}""")

        try {
            llamar { api.registrar(RegistroDto("id", "nuevo", "camila@ejemplo.cl", "Demo1234", true, "1.1", false)) }
            fail("debía lanzar ErrorBackend")
        } catch (e: ErrorBackend) {
            assertEquals(409, e.codigo)
            assertEquals("Ese correo ya está registrado", e.campos["correo"])
        }
    }

    @Test
    fun eliminarConEscaneos_409_RN30() = runTest {
        responder(409, """{"error":"Ya hay usuarios con progreso aquí: desactívalo en vez de eliminarlo","campos":{}}""")

        try {
            llamarSinCuerpo { api.eliminarPunto("p1") }
            fail("debía lanzar ErrorBackend")
        } catch (e: ErrorBackend) {
            assertEquals("Ya hay usuarios con progreso aquí: desactívalo en vez de eliminarlo", e.message)
        }
        assertEquals("DELETE", servidor.takeRequest().method)
    }

    @Test
    fun eliminar_204_terminaSinError() = runTest {
        responder(204)
        llamarSinCuerpo { api.eliminarPunto("p1") }
        assertEquals("/api/puntos/p1", servidor.takeRequest().path)
    }

    @Test
    fun servidorApagado_esSinServidor_EXC06() = runTest {
        servidor.shutdown()

        try {
            llamar { api.puntos() }
            fail("debía lanzar SinServidor")
        } catch (e: SinServidor) {
            assertEquals("No hay conexión con el servidor. Intenta más tarde", e.mensajeParaUsuario("otro"))
        }
    }

    @Test
    fun sincronizarEscaneos_enviaLaFilaSinPuntos_yLeeLosMotivos_EXC08() = runTest {
        responder(200, """[{"id":"e1","aceptado":true,"motivo":null},{"id":"e2","aceptado":false,"motivo":"LEJOS"},
            {"id":"e3","aceptado":false,"motivo":"OTRO_MOTIVO_NUEVO"}]""")
        val fila = DatosPrueba.progreso.first()

        val respuestas = RetrofitServidorApi(api).confirmarEscaneos("cualquiera", listOf(fila))

        val cuerpo = servidor.takeRequest().body.readUtf8()
        assertTrue(cuerpo.contains("\"puntoId\":\"${fila.puntoId}\""))
        //los puntos y el usuario no viajan: el servidor los saca del token y los calcula
        assertFalse(cuerpo.contains("puntosEscaneo"))
        assertFalse(cuerpo.contains("usuarioId"))
        assertTrue(respuestas[0].aceptado)
        assertEquals(MotivoRechazo.LEJOS, respuestas[1].motivo)
        //un motivo desconocido no rompe la app: se trata como duplicado
        assertEquals(MotivoRechazo.DUPLICADO, respuestas[2].motivo)
    }

    @Test
    fun erroresSinCuerpo_tienenUnMensajeSegunElCodigo() {
        assertEquals("No tienes permiso para hacer esto", aErrorBackend(403, null).message)
        assertEquals("El servicio no está disponible; intenta más tarde", aErrorBackend(503, "no es json").message)
        assertEquals("El servidor respondió un error (500)", aErrorBackend(500, "").message)
        assertEquals("No se encontró", aErrorBackend(404, null).message)
        assertEquals("Tu sesión venció. Vuelve a iniciar sesión", aErrorBackend(401, null).message)
        assertEquals("por defecto", IllegalStateException().mensajeParaUsuario("por defecto"))
    }
}
