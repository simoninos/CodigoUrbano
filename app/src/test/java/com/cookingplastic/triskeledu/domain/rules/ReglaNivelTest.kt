package com.cookingplastic.triskeledu.domain.rules

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.data.model.puntosTotales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

//RN-31 a RN-33: niveles desde los puntos y temas desbloqueados
class ReglaNivelTest {

    @Test
    fun umbralesDeLosOchoNiveles() {
        assertEquals(listOf(0, 35, 105, 210, 350, 525, 735, 980), (1..8).map { ReglaNivel.umbral(it) })
        //sin tope: el nivel 9 llega con 1260
        assertEquals(1260, ReglaNivel.umbral(9))
        assertEquals(9, ReglaNivel.numeroDesde(1260))
        assertEquals(8, ReglaNivel.numeroDesde(1259))
    }

    @Test
    fun nivelSegunLosPuntos() {
        assertEquals(1, ReglaNivel.numeroDesde(0))
        assertEquals(1, ReglaNivel.numeroDesde(34))
        assertEquals(2, ReglaNivel.numeroDesde(35))
        val nivel = ReglaNivel.nivelDesde(160)
        assertEquals(3, nivel.numero)
        assertEquals("Descubridor", nivel.nombre)
        assertEquals(105, nivel.puntosDesde)
        assertEquals(210, nivel.puntosSiguiente)
        assertEquals(160, nivel.puntosTotales)
    }

    @Test
    fun desdeElOchoSiempreEsMaestroIa() {
        assertEquals("Maestro IA", ReglaNivel.nombre(8))
        assertEquals("Maestro IA", ReglaNivel.nombre(9))
        assertEquals("Maestro IA", ReglaNivel.nivelDesde(5000).nombre)
        assertEquals("Visitante", ReglaNivel.nombre(1))
    }

    @Test
    fun temasDesbloqueados() {
        //RN-32: el nivel 3 tiene los tres primeros temas
        assertEquals(
            setOf(TemaApp.CLASICO, TemaApp.CERRO_SANTA_LUCIA, TemaApp.ATARDECER_MAPOCHO),
            ReglaNivel.temasDesbloqueados(3, 1, RolUsuario.CLIENTE)
        )
        //RN-33: si bajó al nivel 2 pero llegó al 4, conserva los del 4
        assertEquals(4, ReglaNivel.temasDesbloqueados(2, 4, RolUsuario.CLIENTE).size)
        //del 9 en adelante no hay más temas
        assertEquals(8, ReglaNivel.temasDesbloqueados(12, 12, RolUsuario.CLIENTE).size)
        assertNull(ReglaNivel.temaDelNivel(9))
        //LVL-05: el administrador tiene los 8 sin tener nivel
        assertEquals(TemaApp.entries.toSet(), ReglaNivel.temasDesbloqueados(1, 1, RolUsuario.ADMINISTRADOR))
    }

    @Test
    fun cadaUsuarioDePruebaQuedaEnSuNivel() {
        val esperado = mapOf(
            DatosPrueba.USUARIO_NUEVA to 1,
            DatosPrueba.USUARIO_EXPLORADOR to 2,
            DatosPrueba.USUARIO_CAMILA to 3,
            DatosPrueba.USUARIO_TOMAS to 4,
            DatosPrueba.USUARIO_CONTENIDO to 5,
            DatosPrueba.USUARIO_GUARDIAN to 6,
            DatosPrueba.USUARIO_SABIO to 7,
            DatosPrueba.USUARIO_MAESTRO to 8,
        )
        val preferencias = DatosPrueba.preferencias.associateBy { it.usuarioId }
        esperado.forEach { (usuario, nivel) ->
            val puntos = DatosPrueba.progreso.filter { it.usuarioId == usuario.id }.sumOf { it.puntosTotales() }
            assertEquals(usuario.username, nivel, ReglaNivel.numeroDesde(puntos))
            //así no aparece "¡Subiste de nivel!" al entrar por primera vez (RN-34)
            assertEquals(usuario.username, nivel, preferencias.getValue(usuario.id).nivelMaximoAlcanzado)
        }
        assertEquals(10, DatosPrueba.usuarios.size)
        assertEquals(RolUsuario.CREADOR, DatosPrueba.USUARIO_CONTENIDO.rol)
    }
}
