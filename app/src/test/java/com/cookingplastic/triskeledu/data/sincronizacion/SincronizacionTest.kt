package com.cookingplastic.triskeledu.data.sincronizacion

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.puntosTotales
import com.cookingplastic.triskeledu.data.notificaciones.AvisoLocal
import com.cookingplastic.triskeledu.data.notificaciones.GeneradorNotificaciones
import com.cookingplastic.triskeledu.data.repository.FakeComentarioRepository
import com.cookingplastic.triskeledu.data.repository.FakeNotificacionRepository
import com.cookingplastic.triskeledu.data.repository.FakePreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.FakeUsuarioRepository
import com.cookingplastic.triskeledu.data.simulacion.ReglaSincronizacion
import com.cookingplastic.triskeledu.data.simulacion.ServidorSimulado
import com.cookingplastic.triskeledu.domain.rules.ReglaNivel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

//SIM-03, ESC-02, BKD-06, EXC-08 y EXC-12: el servidor simulado revalida lo guardado sin conexión
class SincronizacionTest {

    private val laMoneda = DatosPrueba.PUNTO_LA_MONEDA
    private val catedral = DatosPrueba.PUNTO_CATEDRAL
    private val puntos = DatosPrueba.puntos.associateBy { it.id }

    private fun escaneo(id: String, usuarioId: String, puntoId: String, donde: Ubicacion, sincronizado: Boolean = false, hace: Long = 0) =
        ProgresoPunto(id = id, usuarioId = usuarioId, puntoId = puntoId, escaneoUbicacion = donde, sincronizado = sincronizado, escaneadoEn = 1_000_000L - hace)

    @Test
    fun aceptaDentroDe100MetrosYRechazaMasLejos() {
        val pendientes = listOf(
            escaneo("cerca", "u", laMoneda.id, DatosPrueba.UBICACION_CERCA_LA_MONEDA),
            escaneo("exacto", "u", DatosPrueba.PUNTO_PRECOLOMBINO.id, DatosPrueba.PUNTO_PRECOLOMBINO.ubicacion),
            escaneo("lejos", "u2", laMoneda.id, DatosPrueba.UBICACION_BORDE_FUERA)
        )
        val respuestas = ReglaSincronizacion.revisarEscaneos(pendientes, confirmados = emptyList(), puntos = puntos).associateBy { it.id }

        assertTrue(respuestas.getValue("cerca").aceptado)
        assertTrue(respuestas.getValue("exacto").aceptado)
        //101,2 m: fuera del radio
        assertEquals(MotivoRechazo.LEJOS, respuestas.getValue("lejos").motivo)
    }

    @Test
    fun rechazaDuplicados() {
        val yaConfirmado = escaneo("viejo", "u", laMoneda.id, laMoneda.ubicacion, sincronizado = true)
        val pendientes = listOf(
            //el servidor ya tenía La Moneda para "u"
            escaneo("repetido", "u", laMoneda.id, laMoneda.ubicacion),
            //dos pendientes del mismo lugar: vale el primero
            escaneo("primero", "u", catedral.id, catedral.ubicacion, hace = 10),
            escaneo("segundo", "u", catedral.id, catedral.ubicacion)
        )
        val respuestas = ReglaSincronizacion.revisarEscaneos(pendientes, listOf(yaConfirmado), puntos).associateBy { it.id }

        assertEquals(MotivoRechazo.DUPLICADO, respuestas.getValue("repetido").motivo)
        assertTrue(respuestas.getValue("primero").aceptado)
        assertEquals(MotivoRechazo.DUPLICADO, respuestas.getValue("segundo").motivo)
    }

    @Test
    fun unaFilaVueltaAEnviarNoChocaConsigoMisma() {
        //vio la cápsula de un lugar ya confirmado: la fila vuelve a sincronizado = false y no es un duplicado
        val respuestas = ReglaSincronizacion.revisarEscaneos(
            listOf(escaneo("misma", "u", laMoneda.id, laMoneda.ubicacion)),
            listOf(escaneo("misma", "u", laMoneda.id, laMoneda.ubicacion, sincronizado = true)),
            puntos
        )
        assertTrue(respuestas.single().aceptado)
    }

    //---------- Sincronizador completo con los Fake ----------

    private class Escenario(filas: List<ProgresoPunto>, comentarios: List<Comentario> = emptyList()) {
        val usuarios = FakeUsuarioRepository()
        val progreso = FakeProgresoRepository(filas)
        val comentarios = FakeComentarioRepository(comentarios)
        val notificaciones = FakeNotificacionRepository()
        val puntosRepo = FakePuntoInteresRepository()
        val avisosSistema = mutableListOf<String>()
        val sincronizador = Sincronizador(
            ServidorSimulado(progreso, puntosRepo, latenciaMs = 0),
            progreso,
            this.comentarios,
            puntosRepo,
            usuarios,
            GeneradorNotificaciones(
                notificaciones,
                object : AvisoLocal { override fun mostrar(tipo: TipoNotificacion, titulo: String, texto: String) { avisosSistema += titulo } },
                usuarios,
                FakePreferenciasRepository()
            )
        )

        suspend fun entrar(correo: String) = usuarios.iniciarSesion(correo, DatosPrueba.CLAVE_DE_PRUEBA)
    }

    @Test
    fun quitaSoloLosPuntosRechazadosYAvisa() = runBlocking {
        val camila = DatosPrueba.USUARIO_CAMILA
        val bien = escaneo("bien", camila.id, laMoneda.id, DatosPrueba.UBICACION_CERCA_LA_MONEDA)
        val mal = escaneo("mal", camila.id, catedral.id, DatosPrueba.UBICACION_LEJOS_CATEDRAL).copy(puntosCapsula = 50)
        val antiguo = escaneo("antiguo", camila.id, DatosPrueba.PUNTO_PRECOLOMBINO.id, DatosPrueba.PUNTO_PRECOLOMBINO.ubicacion, sincronizado = true)
        val escenario = Escenario(listOf(bien, mal, antiguo))
        escenario.entrar(camila.correo)

        val resumen = escenario.sincronizador.sincronizar()

        assertEquals(1, resumen.aceptados)
        assertEquals(1, resumen.rechazados)
        val quedan = escenario.progreso.progresoDe(camila.id).first().associateBy { it.id }
        assertTrue(quedan.getValue("bien").sincronizado)
        assertTrue(quedan.getValue("antiguo").sincronizado)
        //EXC-08: se quitan los 60 puntos de la Catedral; el resto se mantiene
        assertNull(quedan["mal"])
        assertEquals(20, escenario.progreso.totalPuntos(camila.id).first())
        val aviso = escenario.notificaciones.todas.single()
        assertEquals("No pudimos validar tu visita a ${catedral.nombre}", aviso.titulo)
        assertEquals(catedral.id, aviso.puntoId)
        assertTrue(aviso.texto.contains("60 puntos"))
        //es el usuario conectado: también aparece en el sistema
        assertEquals(listOf(aviso.titulo), escenario.avisosSistema)

        //una segunda sincronización ya no tiene nada que enviar
        assertEquals(0, escenario.sincronizador.sincronizar().aceptados)
    }

    @Test
    fun losComentariosTambienSeConfirman() = runBlocking {
        val tomas = DatosPrueba.USUARIO_TOMAS
        val valido = Comentario(id = "c1", usuarioId = tomas.id, puntoId = laMoneda.id, texto = "Muy bueno")
        val vacio = Comentario(id = "c2", usuarioId = tomas.id, puntoId = laMoneda.id, texto = "   ")
        val escenario = Escenario(emptyList(), listOf(valido, vacio))
        escenario.entrar(tomas.correo)

        escenario.sincronizador.sincronizar()

        val quedan = escenario.comentarios.comentariosDe(laMoneda.id).first()
        assertEquals(listOf("c1"), quedan.map { it.id })
        assertTrue(quedan.single().sincronizado)
        assertTrue(escenario.comentarios.pendientesDe(tomas.id).isEmpty())
    }

    @Test
    fun soloSeEnviaLoDeLaCuentaConectada() = runBlocking {
        val camila = DatosPrueba.USUARIO_CAMILA
        val ajeno = escaneo("ajeno", camila.id, catedral.id, DatosPrueba.UBICACION_LEJOS_CATEDRAL)
        val escenario = Escenario(listOf(ajeno))

        //sin sesión no se envía nada
        assertEquals(0, escenario.sincronizador.sincronizar().rechazados)
        //otra cuenta tampoco envía lo de Camila
        escenario.entrar(DatosPrueba.USUARIO_TOMAS.correo)
        assertEquals(0, escenario.sincronizador.sincronizar().rechazados)
        assertFalse(escenario.progreso.progresoDe(camila.id).first().isEmpty())
    }

    @Test
    fun exploradorDemoBajaDeNivelYConservaSuTema() = runBlocking {
        //EXC-12 con los datos de prueba: la Catedral de explorador_demo se guardó a 300 m sin conexión
        val explorador = DatosPrueba.USUARIO_EXPLORADOR
        val escenario = Escenario(DatosPrueba.progreso)
        escenario.entrar(explorador.correo)
        assertEquals(2, ReglaNivel.numeroDesde(escenario.progreso.totalPuntos(explorador.id).first()))

        escenario.sincronizador.sincronizar()

        val puntosDespues = escenario.progreso.totalPuntos(explorador.id).first()
        assertEquals(10, puntosDespues)
        assertEquals(1, ReglaNivel.numeroDesde(puntosDespues))
        //RN-33: el nivel más alto guardado sigue en 2, así que el tema del nivel 2 sigue desbloqueado
        val maximo = DatosPrueba.preferencias.single { it.usuarioId == explorador.id }.nivelMaximoAlcanzado
        val temas = ReglaNivel.temasDesbloqueados(1, maximo, RolUsuario.CLIENTE)
        assertTrue(ReglaNivel.temaDelNivel(2) in temas)
        assertTrue(TemaApp.CLASICO in temas)
        //los demás usuarios de prueba ya estaban confirmados
        assertTrue(DatosPrueba.progreso.filter { it.usuarioId != explorador.id }.all { it.sincronizado })
        assertEquals(70, DatosPrueba.progreso.filter { it.usuarioId == explorador.id }.sumOf { it.puntosTotales() })
    }
}
