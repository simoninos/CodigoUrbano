package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Sesion
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.data.sincronizacion.MotivoRechazo
import com.cookingplastic.triskeledu.ui.state.PeriodoRanking
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

//Prueba de integración: la misma pila de la app (Retrofit, UsuariosConBackend, PuntosConBackend y
//RetrofitServidorApi) contra el backend de verdad, con MySQL. Se salta sola si el gateway no está encendido,
//así "gradlew test" funciona en cualquier computador. Para correrla: Backend\demo\iniciar-todo.ps1
class IntegracionBackendTest {

    companion object {
        private const val GATEWAY = "http://localhost:8080/"

        @JvmStatic
        @BeforeClass
        fun soloConElBackendEncendido() {
            val encendido = runCatching { Socket().use { it.connect(InetSocketAddress("localhost", 8080), 300) } }.isSuccess
            assumeTrue("El backend no está encendido: se salta la prueba de integración", encendido)
        }
    }

    //la sesión que guardaría Room: el interceptor lee el token de aquí
    private var sesion: Sesion? = null
    private val api = ClienteBackend.crear(GATEWAY, token = { sesion?.token }, alVencer = { sesion = null })
    private val dao = mockk<UsuarioDao>(relaxed = true).also { d ->
        coEvery { d.guardarSesion(any()) } answers { sesion = firstArg() }
    }
    private val usuarios = UsuariosConBackend(FakeUsuarioRepository(), dao, api)

    @Test
    fun registro_login_crud_escaneos_y_ranking_contraElBackendReal() = runTest {
        // 1) BKD-02: registro en ms-usuarios; el teléfono manda su UUID y el servidor lo respeta
        val nombre = "integ_" + UUID.randomUUID().toString().take(6)
        val nuevo = usuarios.registrar(nombre, "$nombre@ejemplo.cl", "Demo1234", "1.1")
        assertEquals(RolUsuario.CLIENTE, nuevo.rol)
        assertNotNull(usuarios.iniciarSesion("$nombre@ejemplo.cl", "Demo1234"))
        assertEquals(nuevo.id, sesion?.usuarioId)
        assertNotNull(sesion?.token)

        // 2) EXC-08: escaneos sincronizados en ms-progreso, que pregunta la ubicación a ms-contenido
        val moneda = DatosPrueba.PUNTO_LA_MONEDA
        val servidorApi = RetrofitServidorApi(api)
        val cerca = ProgresoPunto(usuarioId = nuevo.id, puntoId = moneda.id, escaneoUbicacion = DatosPrueba.UBICACION_CERCA_LA_MONEDA)
        val lejos = ProgresoPunto(usuarioId = nuevo.id, puntoId = DatosPrueba.puntos.first { it.activo && it.id != moneda.id }.id,
            escaneoUbicacion = Ubicacion(-33.40, -70.55, 0.0))
        val respuestas = servidorApi.confirmarEscaneos(nuevo.id, listOf(cerca, lejos))
        assertTrue(respuestas[0].aceptado)
        assertEquals(MotivoRechazo.LEJOS, respuestas[1].motivo)

        // 3) LUG-06: el ranking de ms-progreso, con el nombre que viene de ms-usuarios
        val (_, mia) = RankingServidor(api).ranking(PeriodoRanking.SIEMPRE, nuevo.id, cantidad = 5)
        assertEquals(nombre, mia?.username)
        assertEquals(10, mia?.puntos)

        // 4) BKD-05: un cliente no puede crear puntos (403)
        val local = FakePuntoInteresRepository()
        val puntos = PuntosConBackend(local, api)
        val punto = PuntoInteres(nombre = "Punto integración $nombre", descripcion = null, tipo = TipoPuntoInteres.AIPC,
            ubicacion = Ubicacion(-33.4378, -70.6505, 560.0), imagenUri = "android.resource://x/drawable/y",
            descubreMasUrl = null, codigoQr = "", capsulaId = null)
        try {
            puntos.guardar(punto)
            fail("un cliente no debía poder crear puntos")
        } catch (e: ErrorBackend) {
            assertEquals(403, e.codigo)
        }
        assertNull(local.obtenerPorId(punto.id).first())

        // 5) BKD-03: el administrador crea (el QR lo asigna el servidor), edita, no puede activarlo sin cápsula y lo elimina
        assertNotNull(usuarios.iniciarSesion("admin@ejemplo.cl", "Demo1234"))
        puntos.guardar(punto)
        val creado = local.obtenerPorId(punto.id).first()!!
        assertTrue(creado.codigoQr.startsWith("TRISKEL-PI-"))
        puntos.guardar(creado.copy(nombre = "${creado.nombre} (editado)"))
        assertTrue(local.obtenerPorId(punto.id).first()!!.nombre.endsWith("(editado)"))
        try {
            puntos.cambiarActivo(punto.id, true)
            fail("sin cápsula no debía activarse (RN-14)")
        } catch (e: ErrorBackend) {
            assertEquals(409, e.codigo)
        }
        puntos.eliminar(punto.id)
        assertNull(local.obtenerPorId(punto.id).first())

        // 6) RN-30: La Moneda tiene escaneos, así que el servidor no deja eliminarla
        try {
            puntos.eliminar(moneda.id)
            fail("La Moneda tiene escaneos: no debía eliminarse")
        } catch (e: ErrorBackend) {
            assertEquals(409, e.codigo)
        }
    }
}
