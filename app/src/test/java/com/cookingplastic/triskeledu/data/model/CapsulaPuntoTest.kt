package com.cookingplastic.triskeledu.data.model

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.repository.FakePuntoInteresRepository
import com.cookingplastic.triskeledu.domain.rules.EnlaceVideo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

//RN-14, RN-20 y RN-27 con los datos de prueba
class CapsulaPuntoTest {

    private val capsulasPorId = DatosPrueba.capsulas.associateBy { it.id }
    private fun capsulaDe(puntoId: String) =
        DatosPrueba.puntos.first { it.id == puntoId }.capsulaId?.let { capsulasPorId[it] }

    @Test
    fun cadaPuntoTieneSuPropiaCapsula() {
        //RN-20: ninguna cápsula se repite entre puntos
        val asociadas = DatosPrueba.puntos.mapNotNull { it.capsulaId }
        assertEquals(asociadas.size, asociadas.toSet().size)
        //16 puntos: 14 activos, todos con cápsula publicada
        assertEquals(16, DatosPrueba.puntos.size)
        val activos = DatosPrueba.puntos.filter { it.activo }
        assertEquals(14, activos.size)
        assertTrue(activos.all { capsulaDe(it.id)?.estado == EstadoCapsula.PUBLICADA })
        //la cápsula en borrador no está en ningún punto
        assertTrue(DatosPrueba.CAPSULA_BORRADOR.id !in asociadas)
        //todas son de contenido_ia y duran 3 minutos como máximo (RN-26)
        assertTrue(DatosPrueba.capsulas.all { it.creadorId == DatosPrueba.USUARIO_CONTENIDO.id })
        assertTrue(DatosPrueba.capsulas.all { it.duracionSegundos in 1..180 })
    }

    @Test
    fun losSeisVideosEstanEnlazadosYElRestoNo() {
        //V1 a V6: cada uno en su punto, con un enlace de Drive que el reproductor puede convertir
        val conVideo = listOf(
            DatosPrueba.CAPSULA_LA_MONEDA, DatosPrueba.CAPSULA_ESTACION_MAPOCHO, DatosPrueba.CAPSULA_PLAZA_ARMAS,
            DatosPrueba.CAPSULA_PRECOLOMBINO, DatosPrueba.CAPSULA_SANTA_LUCIA, DatosPrueba.CAPSULA_MERCADO_CENTRAL
        )
        assertTrue(conVideo.all { EnlaceVideo.idDeDrive(it.urlStream.orEmpty()) != null })
        assertEquals(6, conVideo.map { it.urlStream }.toSet().size)
        //RN-35: las demás siguen sin enlace
        assertEquals(6, DatosPrueba.capsulas.count { it.urlStream != null })
        //las categorías siguen la tabla de videos del ERS 6
        assertEquals(CategoriaCapsula.IA_VIDA_DIARIA, DatosPrueba.CAPSULA_PLAZA_ARMAS.categoria)
        assertEquals(CategoriaCapsula.HISTORIA_PATRIMONIO, DatosPrueba.CAPSULA_PRECOLOMBINO.categoria)
        assertEquals(CategoriaCapsula.IA_RESPONSABLE, DatosPrueba.CAPSULA_SANTA_LUCIA.categoria)
    }

    @Test
    fun estadoQueVeElAdministrador() {
        //RN-27: los tres estados aparecen en los datos de prueba
        val moneda = DatosPrueba.PUNTO_LA_MONEDA
        val almagro = DatosPrueba.PUNTO_PARQUE_ALMAGRO
        val matte = DatosPrueba.PUNTO_PALACIO_MATTE
        assertEquals(EstadoAdminPunto.ACTIVO, EstadoAdminPunto.desde(moneda, capsulaDe(moneda.id)))
        assertEquals(EstadoAdminPunto.INACTIVO, EstadoAdminPunto.desde(almagro, capsulaDe(almagro.id)))
        assertEquals(EstadoAdminPunto.SIN_CAPSULA, EstadoAdminPunto.desde(matte, capsulaDe(matte.id)))
        //una cápsula en borrador no cuenta: el punto sigue Sin cápsula
        val conBorrador = matte.copy(capsulaId = DatosPrueba.CAPSULA_BORRADOR.id)
        assertEquals(EstadoAdminPunto.SIN_CAPSULA, EstadoAdminPunto.desde(conBorrador, DatosPrueba.CAPSULA_BORRADOR))
    }

    @Test
    fun activarExigeCapsulaPublicadaConEnlace() {
        //RN-14: un punto nuevo con cápsula publicada pero sin enlace no se puede activar
        val nueva = DatosPrueba.CAPSULA_LA_MONEDA.copy(
            id = "capsula-nueva", urlStream = null, categoria = CategoriaCapsula.IA_CIUDAD
        )
        val punto = DatosPrueba.PUNTO_PALACIO_MATTE.copy(capsulaId = nueva.id)
        assertFalse(punto.puedeActivarse(nueva))
        assertEquals(listOf("cápsula"), punto.datosFaltantes(nueva))
        //con enlace sí
        val conEnlace = nueva.copy(urlStream = "https://drive.google.com/file/d/abc/view")
        assertTrue(punto.puedeActivarse(conEnlace))
        //en borrador no, aunque tenga enlace
        assertFalse(punto.puedeActivarse(conEnlace.copy(estado = EstadoCapsula.BORRADOR)))
        //sin cápsula tampoco
        assertFalse(DatosPrueba.PUNTO_PALACIO_MATTE.puedeActivarse(null))
    }

    @Test
    fun losPuntosDePruebaSeActivanSinEnlace() = runBlocking {
        //excepción de demo (RN-14 y RN-35): Parque Almagro se activa con su cápsula sin enlace
        val almagro = DatosPrueba.PUNTO_PARQUE_ALMAGRO
        assertNull(capsulaDe(almagro.id)?.urlStream)
        assertTrue(almagro.puedeActivarse(capsulaDe(almagro.id)))

        val repo = FakePuntoInteresRepository()
        assertTrue(repo.cambiarActivo(almagro.id, true))
        assertTrue(repo.obtenerPorId(almagro.id).first()!!.activo)
        //Palacio Matte no tiene cápsula: no se activa
        assertFalse(repo.cambiarActivo(DatosPrueba.PUNTO_PALACIO_MATTE.id, true))
    }
}
