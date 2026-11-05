package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.notificaciones.ReglaAvisos
import com.cookingplastic.triskeledu.ui.validation.ReglaQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

//RNF-03 y EXC-13: cuándo corre "Lugar cercano" con la app minimizada y qué puntos avisa
class SegundoPlanoTest {

    private val camila = DatosPrueba.USUARIO_CAMILA
    private val suyas = Preferencias(usuarioId = camila.id)

    @Test
    fun soloCorreConLosDosInterruptoresYElPermiso() {
        assertTrue(ReglaAvisos.vigilarEnSegundoPlano(camila, suyas, permisoSegundoPlano = true))
        //EXC-13: sin "Permitir todo el tiempo" no corre (sigue con la app abierta)
        assertFalse(ReglaAvisos.vigilarEnSegundoPlano(camila, suyas, permisoSegundoPlano = false))
        assertFalse(ReglaAvisos.vigilarEnSegundoPlano(camila, suyas.copy(notifLugaresCercanos = false), true))
        assertFalse(ReglaAvisos.vigilarEnSegundoPlano(camila, suyas.copy(seguirUbicacion = false), true))
        //sin sesión, el administrador o una cuenta en revisión: no
        assertFalse(ReglaAvisos.vigilarEnSegundoPlano(null, suyas, true))
        assertFalse(ReglaAvisos.vigilarEnSegundoPlano(DatosPrueba.USUARIO_ADMIN, Preferencias(usuarioId = DatosPrueba.USUARIO_ADMIN.id), true))
        assertFalse(ReglaAvisos.vigilarEnSegundoPlano(DatosPrueba.USUARIO_PENDIENTE, Preferencias(usuarioId = DatosPrueba.USUARIO_PENDIENTE.id), true))
        //preferencias de otra cuenta (todavía cargando): no
        assertFalse(ReglaAvisos.vigilarEnSegundoPlano(camila, Preferencias(usuarioId = ""), true))
    }

    @Test
    fun avisaSoloActivosSinEscanearA200MetrosOMenos() {
        //desde la Plaza de Armas: la Catedral está a unos 100 m y La Moneda a 712 m
        val cercanos = ReglaAvisos.cercanosSinEscanear(DatosPrueba.puntos, escaneados = emptySet(), ubicacion = DatosPrueba.UBICACION_PLAZA_DE_ARMAS)
        val ids = cercanos.map { it.first.id }
        assertTrue(DatosPrueba.PUNTO_CATEDRAL.id in ids)
        assertFalse(DatosPrueba.PUNTO_LA_MONEDA.id in ids)
        assertTrue(cercanos.all { it.second <= ReglaAvisos.RADIO_CERCANO_METROS && it.first.activo })

        //si ya la escaneó, no se avisa
        val sinCatedral = ReglaAvisos.cercanosSinEscanear(DatosPrueba.puntos, setOf(DatosPrueba.PUNTO_CATEDRAL.id), DatosPrueba.UBICACION_PLAZA_DE_ARMAS)
        assertFalse(DatosPrueba.PUNTO_CATEDRAL.id in sinCatedral.map { it.first.id })
    }

    @Test
    fun formatoDelQrDelJuego() {
        //EXC-04: distingue un código ajeno de uno del juego cuyo punto ya no existe
        assertTrue(ReglaQr.esCodigoDelJuego("TRISKEL-PI-099"))
        assertTrue(ReglaQr.esCodigoDelJuego(" TRISKEL-PI-001 "))
        assertFalse(ReglaQr.esCodigoDelJuego("https://ejemplo.cl"))
        assertEquals(false, ReglaQr.esCodigoDelJuego("TRISKEL-PI-"))
    }
}
