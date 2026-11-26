package com.cookingplastic.triskeledu.data.sincronizacion

import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.remoto.SinServidor
import com.cookingplastic.triskeledu.data.repository.FakeComentarioRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.ConnectException

//BKD-06: comentar exige conexión; el comentario se guarda en el teléfono solo si el servidor lo acepta
class ComentariosConServidorTest {

    private val servidor = mockk<ServidorApi>()
    private val room = FakeComentarioRepository()
    private val repo = ComentariosConServidor(room, servidor)
    private val comentario = Comentario(usuarioId = "u1", puntoId = "p1", texto = "Muy buena la explicación")

    @Test
    fun aceptado_seGuardaYaSincronizado() = runTest {
        coEvery { servidor.confirmarComentarios("u1", listOf(comentario)) } returns listOf(RespuestaRegistro(comentario.id))

        repo.agregar(comentario)

        assertEquals(listOf(comentario.id), room.comentariosDe("p1").first().map { it.id })
        assertTrue(room.pendientesDe("u1").isEmpty())
    }

    @Test
    fun sinServidor_noSeGuarda_yAvisaQueNecesitaConexion() = runTest {
        coEvery { servidor.confirmarComentarios(any(), any()) } throws SinServidor(ConnectException("apagado"))

        try {
            repo.agregar(comentario)
            fail("sin servidor no debía publicarse")
        } catch (e: SinServidor) {
            assertEquals("No hay conexión con el servidor. Necesitas conexión para comentar", e.message)
        }
        assertTrue(room.comentariosDe("p1").first().isEmpty())
    }

    @Test
    fun rechazado_noSeGuarda_yElMensajeEsElMotivo() = runTest {
        coEvery { servidor.confirmarComentarios(any(), any()) } returns
            listOf(RespuestaRegistro(comentario.id, MotivoRechazo.TEXTO_INVALIDO))

        try {
            repo.agregar(comentario)
            fail("el servidor lo rechazó")
        } catch (e: ComentarioRechazado) {
            assertEquals(MotivoRechazo.TEXTO_INVALIDO.explicacion, e.message)
        }
        assertTrue(room.comentariosDe("p1").first().isEmpty())
    }
}
