package com.cookingplastic.triskeledu.ui.screens.mislugares

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.CategoriaCapsula
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

//LUG-01 a LUG-03 y RN-23: Mis descubrimientos (P07-A a P07-D)
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
        imagenUri = "imagen",
        descubreMasUrl = null,
        codigoQr = id,
        //cada lugar tiene su cápsula, como exige RN-14 para estar activo
        capsulaId = "c-$id",
        activo = true
    )

    private fun capsula(puntoId: String, titulo: String) = Capsula(
        id = "c-$puntoId",
        titulo = titulo,
        urlStream = null,
        categoria = CategoriaCapsula.IA_CIUDAD,
        duracionSegundos = 60,
        estado = EstadoCapsula.PUBLICADA,
        creadorId = "creador"
    )

    private val puntos = listOf(
        punto("p1", "La Moneda"),
        punto("p2", "Museo de Bellas Artes"),
        punto("p3", "Museo de Arte Precolombino"),
        punto("p4", "Cerro Santa Lucía"),
        punto("p5", "Plaza de Armas")
    )

    private val capsulas = listOf(
        capsula("p1", "La IA en el gobierno"),
        capsula("p2", "IA y arte"),
        capsula("p3", "IA y arqueología"),
        capsula("p4", "IA y el paisaje"),
        capsula("p5", "IA y la plaza")
    )

    private val progreso = listOf(
        ProgresoPunto(usuarioId = "u1", puntoId = "p1", escaneoUbicacion = ubicacion, escaneadoEn = ahora),
        ProgresoPunto(usuarioId = "u1", puntoId = "p2", escaneoUbicacion = ubicacion, escaneadoEn = haceDosDias),
        ProgresoPunto(
            usuarioId = "u1", puntoId = "p3", escaneoUbicacion = ubicacion, escaneadoEn = haceDosDias,
            capsulaVistaEn = haceDosDias, puntosCapsula = 50, selfieUri = "selfie", selfieEn = haceDosDias, puntosSelfie = 20
        ),
        ProgresoPunto(usuarioId = "u1", puntoId = "p5", escaneoUbicacion = ubicacion, escaneadoEn = haceDosDias, capsulaVistaEn = haceDosDias, puntosCapsula = 50)
    )

    private fun armar(
        vista: VistaDescubrimientos = VistaDescubrimientos.LUGARES,
        filtro: FiltroLugares = FiltroLugares.TODOS,
        orden: OrdenLugares = OrdenLugares.RECIENTES
    ) = armarMisLugares(puntos, progreso, filtro, orden, ahora, capsulas, vista)

    @Test
    fun cuentaPuntosYLugares() {
        val estado = armar()

        //10 + 10 + 80 + 60
        assertEquals(160, estado.totalPuntos)
        assertEquals(4, estado.lugaresEscaneados)
        assertEquals(5, estado.totalLugares)
        assertEquals(listOf("Cerro Santa Lucía"), estado.porDescubrir.map { it.nombre })
    }

    @Test
    fun cuentaQrCapsulasYSelfies() {
        val estado = armar()

        //LUG-01: "X de Y" en los tres cuadros; Y es el total de lugares activos
        assertEquals(4, estado.qrEscaneados)
        assertEquals(2, estado.capsulasVistas)
        assertEquals(1, estado.selfiesTomadas)
        assertEquals(5, estado.totalLugares)
    }

    @Test
    fun calculaElEstadoDeCadaLugar() {
        val estado = armar(orden = OrdenLugares.NOMBRE)
        val porNombre = estado.desbloqueados.associateBy { it.nombre }

        assertEquals(EstadoLugar.RECIEN_DESCUBIERTO, porNombre.getValue("La Moneda").estado)
        assertEquals(EstadoLugar.FALTA_CAPSULA, porNombre.getValue("Museo de Bellas Artes").estado)
        assertEquals(EstadoLugar.FALTA_SELFIE, porNombre.getValue("Plaza de Armas").estado)
        assertEquals(EstadoLugar.COMPLETADO, porNombre.getValue("Museo de Arte Precolombino").estado)
        //el máximo de la esfera es 80
        assertEquals(80, porNombre.getValue("La Moneda").maximo)
    }

    @Test
    fun soloListaLoDesbloqueado() {
        val estado = armar(vista = VistaDescubrimientos.CAPSULAS)

        //RN-23: la cápsula de Santa Lucía no aparece porque su QR no se ha escaneado
        assertEquals(4, estado.capsulas.size)
        assertFalse(estado.capsulas.any { it.puntoId == "p4" })
        assertFalse(estado.qrs.any { it.puntoId == "p4" })
        //solo hay selfie en Precolombino
        assertEquals(listOf("Museo de Arte Precolombino"), estado.selfies.map { it.nombre })
        assertEquals("selfie", estado.selfies.single().selfieUri)
    }

    @Test
    fun marcaLasCapsulasVistasYPorVer() {
        val estado = armar(vista = VistaDescubrimientos.CAPSULAS, orden = OrdenLugares.NOMBRE)
        val porTitulo = estado.capsulas.associateBy { it.titulo }

        assertFalse(porTitulo.getValue("La IA en el gobierno").vista)
        assertTrue(porTitulo.getValue("IA y arqueología").vista)
        assertEquals("La Moneda", porTitulo.getValue("La IA en el gobierno").nombrePunto)
        assertEquals("IA y la ciudad", porTitulo.getValue("La IA en el gobierno").categoria)
    }

    @Test
    fun enCursoMuestraLasCapsulasPorVer() {
        val estado = armar(vista = VistaDescubrimientos.CAPSULAS, filtro = FiltroLugares.EN_CURSO, orden = OrdenLugares.NOMBRE)

        assertEquals(listOf("IA y arte", "La IA en el gobierno"), estado.capsulas.map { it.titulo })
    }

    @Test
    fun porDescubrirCambiaConLaVista() {
        //lugares, cápsulas y QR: lo que falta escanear
        assertEquals(listOf("p4"), armar(vista = VistaDescubrimientos.QR).porDescubrir.map { it.puntoId })
        assertEquals(listOf("p4"), armar(vista = VistaDescubrimientos.CAPSULAS).porDescubrir.map { it.puntoId })
        //selfies: todo lugar sin selfie, aunque ya esté escaneado; ordenados por nombre
        val selfies = armar(vista = VistaDescubrimientos.SELFIES).porDescubrir.map { it.nombre }
        assertEquals(listOf("Cerro Santa Lucía", "La Moneda", "Museo de Bellas Artes", "Plaza de Armas"), selfies)
        assertEquals("Selfie en La Moneda", VistaDescubrimientos.SELFIES.textoPorDescubrir("La Moneda"))
    }

    @Test
    fun guardaLaDistanciaDelEscaneo() {
        val lejos = Ubicacion(-33.44, -70.6505, 560.0)
        val conDistancia = listOf(ProgresoPunto(usuarioId = "u1", puntoId = "p1", escaneoUbicacion = lejos, escaneadoEn = ahora))
        val estado = armarMisLugares(puntos, conDistancia, FiltroLugares.TODOS, OrdenLugares.RECIENTES, ahora, capsulas, VistaDescubrimientos.QR)

        //0,0005° de longitud a esta latitud son unos 46 m
        val distancia = estado.qrs.single().distanciaMetros
        assertTrue("distancia: $distancia", distancia in 44..48)
    }

    @Test
    fun filtraLosCompletados() {
        val estado = armar(filtro = FiltroLugares.COMPLETADOS)

        assertEquals(listOf("Museo de Arte Precolombino"), estado.desbloqueados.map { it.nombre })
        assertEquals(listOf("p3"), estado.qrs.map { it.puntoId })
        //los totales no cambian con el filtro
        assertEquals(160, estado.totalPuntos)
        assertEquals(4, estado.qrEscaneados)
    }

    @Test
    fun ordenaPorPuntos() {
        val estado = armar(orden = OrdenLugares.PUNTOS)

        assertEquals("Museo de Arte Precolombino", estado.desbloqueados.first().nombre)
        assertEquals("p3", estado.qrs.first().puntoId)
    }
}
