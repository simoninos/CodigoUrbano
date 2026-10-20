package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.ui.state.EstadoLugar
import com.cookingplastic.triskeledu.ui.state.FiltroLugares
import com.cookingplastic.triskeledu.ui.state.OrdenLugares
import com.cookingplastic.triskeledu.ui.viewmodels.armarMisLugares
import org.junit.Assert.assertEquals
import org.junit.Test

class MisLugaresTest {

    private val ahora = 1_000_000_000_000L
    private val haceDosDias = ahora - 2 * 24 * 60 * 60 * 1000L
    private val ubicacion = Ubicacion(-33.44, -70.65, 560.0)

    private fun punto(id: String, nombre: String) = PuntoInteres(
        id = id,
        nombre = nombre,
        descripcion = null,
        tipo = TipoPuntoInteres.MONUMENTO,
        ubicacion = ubicacion,
        imagenUri = null,
        descubreMasUrl = null,
        codigoQr = null,
        capsulaId = null,
        activo = true
    )

    private val puntos = listOf(
        punto("p1", "La Moneda"),
        punto("p2", "Museo de Bellas Artes"),
        punto("p3", "Museo de Arte Precolombino"),
        punto("p4", "Cerro Santa Lucía")
    )

    private val progreso = listOf(
        ProgresoPunto(usuarioId = "u1", puntoId = "p1", escaneoUbicacion = ubicacion, escaneadoEn = ahora),
        ProgresoPunto(usuarioId = "u1", puntoId = "p2", escaneoUbicacion = ubicacion, escaneadoEn = haceDosDias),
        ProgresoPunto(usuarioId = "u1", puntoId = "p3", escaneoUbicacion = ubicacion, escaneadoEn = haceDosDias, puntosVideo = 15)
    )

    @Test
    fun cuentaPuntosYLugares() {
        val estado = armarMisLugares(puntos, progreso, FiltroLugares.TODOS, OrdenLugares.RECIENTES, ahora)

        assertEquals(30, estado.totalPuntos)
        assertEquals(3, estado.lugaresEscaneados)
        assertEquals(4, estado.totalLugares)
        assertEquals(1, estado.bloqueados)
    }

    @Test
    fun calculaElEstadoDeCadaLugar() {
        val estado = armarMisLugares(puntos, progreso, FiltroLugares.TODOS, OrdenLugares.NOMBRE, ahora)
        val porNombre = estado.desbloqueados.associateBy { it.nombre }

        assertEquals(EstadoLugar.RECIEN_DESCUBIERTO, porNombre.getValue("La Moneda").estado)
        assertEquals(EstadoLugar.FALTA_VIDEO, porNombre.getValue("Museo de Bellas Artes").estado)
        assertEquals(EstadoLugar.COMPLETADO, porNombre.getValue("Museo de Arte Precolombino").estado)
    }

    @Test
    fun filtraLosCompletados() {
        val estado = armarMisLugares(puntos, progreso, FiltroLugares.COMPLETADOS, OrdenLugares.RECIENTES, ahora)

        assertEquals(listOf("Museo de Arte Precolombino"), estado.desbloqueados.map { it.nombre })
        //los totales no cambian con el filtro
        assertEquals(30, estado.totalPuntos)
    }

    @Test
    fun ordenaPorPuntos() {
        val estado = armarMisLugares(puntos, progreso, FiltroLugares.TODOS, OrdenLugares.PUNTOS, ahora)

        assertEquals("Museo de Arte Precolombino", estado.desbloqueados.first().nombre)
    }
}