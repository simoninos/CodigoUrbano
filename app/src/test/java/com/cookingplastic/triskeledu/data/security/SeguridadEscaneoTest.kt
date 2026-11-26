package com.cookingplastic.triskeledu.data.security

import androidx.sqlite.db.SupportSQLiteDatabase
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.local.MIGRACION_3_4
import com.cookingplastic.triskeledu.data.location.FakeUbicacionRepository
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.remoto.BackendApi
import com.cookingplastic.triskeledu.data.remoto.ClienteBackend
import com.cookingplastic.triskeledu.data.remoto.RetrofitServidorApi
import com.cookingplastic.triskeledu.data.remoto.UsuariosConBackend
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.data.sincronizacion.MotivoRechazo
import com.cookingplastic.triskeledu.domain.rules.ReglaQr
import com.cookingplastic.triskeledu.ui.screens.escanear.ProcesadorEscaneo
import com.cookingplastic.triskeledu.ui.screens.escanear.ResultadoEscaneo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

//SEG-01, SEG-03 y SEG-05 del lado de la app: GPS simulado, QR firmado, firma de cada escaneo y Key Attestation al
//iniciar sesión. La clave del teléfono se reemplaza por una de prueba, porque AndroidKeyStore solo existe en Android
class SeguridadEscaneoTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: BackendApi

    //clave de prueba: devuelve una cadena fija y firma con un texto reconocible
    private class ClaveDePrueba(private val cadena: List<String>? = listOf("hoja", "raiz")) : ClaveDispositivo {
        var desafioRecibido: String? = null
        override fun crear(desafio: String): List<String>? {
            desafioRecibido = desafio
            return cadena
        }

        override fun firmar(texto: String): String = "firma(" + texto + ")"
    }

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        api = ClienteBackend.crear(servidor.url("/").toString(), token = { null }, alVencer = { })
    }

    @After
    fun cerrar() {
        runCatching { servidor.shutdown() }
    }

    private fun responder(codigo: Int, cuerpo: String = "") =
        servidor.enqueue(MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo))

    private val sesion = """{"token":"t","expiraEn":1,"cuentaEnRevision":false,
        "usuario":{"id":"u1","username":"camila_explora","correo":"camila@ejemplo.cl","rol":"CLIENTE",
        "terminosVersion":"1.1","terminosAceptadosEn":1,"creadoEn":2,"solicitudCreador":null}}"""

    // ---------- SEG-03: QR firmado ----------

    @Test
    fun elQrFirmado_seSeparaDeSuFirma_ySigueSiendoDelJuego() {
        assertEquals("TRISKEL-PI-001", ReglaQr.sinFirma("TRISKEL-PI-001.AbC_123-xyz"))
        assertEquals("TRISKEL-PI-001", ReglaQr.sinFirma("  TRISKEL-PI-001  "))
        assertTrue(ReglaQr.esCodigoDelJuego("TRISKEL-PI-099.firma"))
        assertFalse(ReglaQr.esCodigoDelJuego("OTRO.QR"))
        //un punto firmado no hace que el siguiente código se repita
        assertEquals("TRISKEL-PI-018", ReglaQr.siguienteCodigo(listOf("TRISKEL-PI-017.firma", "TRISKEL-PI-003")))
    }

    @Test
    fun escanearUnQrFirmado_desbloquea_yGuardaElCodigoCompletoYLaPrecision() = runTest {
        val progreso = FakeProgresoRepository()
        val procesador = ProcesadorEscaneo(FakePuntoInteresRepository(), FakeUbicacionRepository(), progreso)

        val resultado = procesador.procesar("TRISKEL-PI-001.AbC_123-xyz", "u1")

        assertTrue(resultado is ResultadoEscaneo.Desbloqueado)
        val fila = progreso.progresoDe("u1").first().single()
        assertEquals("TRISKEL-PI-001.AbC_123-xyz", fila.codigoQrLeido)
        assertEquals(8.0, fila.escaneoPrecision!!, 0.0)
        assertFalse(fila.escaneoSimulado)
    }

    // ---------- SEG-01: GPS simulado ----------

    @Test
    fun conUnaAppDeUbicacionFalsa_noSeEscanea_niSuma() = runTest {
        val progreso = FakeProgresoRepository()
        val gps = FakeUbicacionRepository().apply { simulada = true }
        val procesador = ProcesadorEscaneo(FakePuntoInteresRepository(), gps, progreso)

        val resultado = procesador.procesar("TRISKEL-PI-001", "u1")

        assertEquals(ResultadoEscaneo.Rechazado(ProcesadorEscaneo.UBICACION_SIMULADA), resultado)
        assertEquals(0, progreso.totalPuntos("u1").first())
    }

    // ---------- SEG-05: firma de cada escaneo ----------

    private val fila = ProgresoPunto(
        id = "e1", usuarioId = "u1", puntoId = "p1", escaneadoEn = 1_700_000_000_000,
        escaneoUbicacion = Ubicacion(-33.4429, -70.6539, 570.0), escaneoPrecision = 6.5,
        codigoQrLeido = "TRISKEL-PI-001.firma"
    )

    @Test
    fun elTextoFirmado_esElMismoQueArmaMsProgreso() {
        //FirmaEscaneo.textoFirmado en ms-progreso: id|puntoId|escaneadoEn|latitud|longitud|codigoQr|simulada
        assertEquals("e1|p1|1700000000000|-33.4429|-70.6539|TRISKEL-PI-001.firma|false", textoFirmado(fila))
        assertEquals("e1|p1|1700000000000|-33.4429|-70.6539||false", textoFirmado(fila.copy(codigoQrLeido = null)))
    }

    @Test
    fun alSincronizar_cadaEscaneoVaFirmado_conSusDatosDeSeguridad_yLeeLosMotivosNuevos() = runTest {
        responder(200, """[{"id":"e1","aceptado":false,"motivo":"QR_INVALIDO"}]""")

        val respuestas = RetrofitServidorApi(api, ClaveDePrueba()).confirmarEscaneos("u1", listOf(fila))

        val cuerpo = servidor.takeRequest().body.readUtf8()
        assertTrue(cuerpo.contains("\"simulada\":false"))
        assertTrue(cuerpo.contains("\"precision\":6.5"))
        assertTrue(cuerpo.contains("\"codigoQr\":\"TRISKEL-PI-001.firma\""))
        assertTrue(cuerpo.contains("\"firma\":\"firma(e1|p1|1700000000000|-33.4429|-70.6539|TRISKEL-PI-001.firma|false)\""))
        assertEquals(MotivoRechazo.QR_INVALIDO, respuestas.single().motivo)
    }

    // ---------- SEG-05: Key Attestation al iniciar sesión ----------

    @Test
    fun login_pideUnDesafio_creaLaClaveAtadaAEl_yMandaLaCadena() = runTest {
        responder(200, """{"desafio":"d-123","expiraEn":5}""")
        responder(200, sesion)
        val clave = ClaveDePrueba()
        val repo = UsuariosConBackend(FakeUsuarioRepository(), mockk(relaxed = true), api, clave = clave)

        repo.iniciarSesion("camila@ejemplo.cl", "Demo1234")

        assertEquals("/api/auth/desafio", servidor.takeRequest().path)
        val login = servidor.takeRequest()
        assertEquals("/api/auth/login", login.path)
        assertEquals("d-123", clave.desafioRecibido)
        assertEquals("""{"correo":"camila@ejemplo.cl","clave":"Demo1234","desafio":"d-123","atestacion":["hoja","raiz"]}""",
            login.body.readUtf8())
    }

    @Test
    fun login_siElServidorNoDaDesafio_oElTelefonoNoCreaLaClave_entraIgualSinAtestacion() = runTest {
        //servidor sin el endpoint (404)
        responder(404)
        responder(200, sesion)
        val repo = UsuariosConBackend(FakeUsuarioRepository(), mockk(relaxed = true), api, clave = ClaveDePrueba())

        assertEquals("u1", repo.iniciarSesion("camila@ejemplo.cl", "Demo1234")?.id)
        servidor.takeRequest()
        assertEquals("""{"correo":"camila@ejemplo.cl","clave":"Demo1234"}""", servidor.takeRequest().body.readUtf8())

        //teléfono sin chip de seguridad: va el desafío, pero sin cadena
        responder(200, """{"desafio":"d-9","expiraEn":5}""")
        responder(200, sesion)
        val sinChip = UsuariosConBackend(FakeUsuarioRepository(), mockk(relaxed = true), api, clave = ClaveDePrueba(cadena = null))

        assertEquals("u1", sinChip.iniciarSesion("camila@ejemplo.cl", "Demo1234")?.id)
        servidor.takeRequest()
        assertFalse(servidor.takeRequest().body.readUtf8().contains("atestacion"))
    }

    // ---------- Room v3 → v4 ----------

    @Test
    fun laMigracion_soloAgregaLasTresColumnasNuevas_conLosNombresDeLaEntidad() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val sentencias = mutableListOf<String>()
        every { db.execSQL(any<String>()) } answers { sentencias += firstArg<String>() }

        MIGRACION_3_4.migrate(db)

        assertEquals(3, MIGRACION_3_4.startVersion)
        assertEquals(4, MIGRACION_3_4.endVersion)
        assertEquals(
            listOf(
                "ALTER TABLE progreso_puntos ADD COLUMN escaneoSimulado INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE progreso_puntos ADD COLUMN escaneoPrecision REAL",
                "ALTER TABLE progreso_puntos ADD COLUMN codigoQrLeido TEXT"
            ),
            sentencias
        )
        //los nombres de columna son los de los campos de ProgresoPunto (Room usa el nombre del campo)
        val campos = ProgresoPunto::class.java.declaredFields.map { it.name }
        listOf("escaneoSimulado", "escaneoPrecision", "codigoQrLeido").forEach { assertTrue(it, it in campos) }
        verify(exactly = 3) { db.execSQL(any<String>()) }
        assertNull(DatosPrueba.progreso.first().codigoQrLeido)
    }
}
