package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.PUNTOS_CAPSULA
import com.cookingplastic.triskeledu.data.model.PUNTOS_ESCANEO
import com.cookingplastic.triskeledu.data.model.PUNTOS_MAXIMOS_LUGAR
import com.cookingplastic.triskeledu.data.model.PUNTOS_SELFIE
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.puntosTotales
import com.cookingplastic.triskeledu.ui.state.DetalleUiState
import org.junit.Assert.assertEquals
import org.junit.Test

//RN-01 a RN-03 y RN-16: QR 10, cápsula 50 y selfie 20, con un máximo de 80 por lugar
class PuntajesTest {

    private val aqui = Ubicacion(-33.44, -70.65, 560.0)

    @Test
    fun cadaActividadDaSusPuntos() {
        assertEquals(10, PUNTOS_ESCANEO)
        assertEquals(50, PUNTOS_CAPSULA)
        assertEquals(20, PUNTOS_SELFIE)
        assertEquals(80, PUNTOS_MAXIMOS_LUGAR)
    }

    @Test
    fun soloConLasTresActividadesQuedaCompletado() {
        val escaneado = ProgresoPunto(usuarioId = "u1", puntoId = "p1", escaneoUbicacion = aqui)
        val conSelfie = escaneado.copy(puntosSelfie = PUNTOS_SELFIE)
        val completo = conSelfie.copy(puntosCapsula = PUNTOS_CAPSULA)

        assertEquals(EstadoPunto.DESBLOQUEADO, EstadoPunto.desde(escaneado))
        assertEquals(30, conSelfie.puntosTotales())
        assertEquals(EstadoPunto.DESBLOQUEADO, EstadoPunto.desde(conSelfie))
        assertEquals(80, completo.puntosTotales())
        assertEquals(EstadoPunto.COMPLETADO, EstadoPunto.desde(completo))
        assertEquals(EstadoPunto.BLOQUEADO, EstadoPunto.desde(null))
    }

    @Test
    fun laEsferaDelDetalleLlegaA80() {
        assertEquals(80, DetalleUiState().puntosMaximos)
    }
}
