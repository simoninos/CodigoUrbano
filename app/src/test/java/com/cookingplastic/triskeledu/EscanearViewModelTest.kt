package com.cookingplastic.triskeledu

import com.cookingplastic.triskeledu.ui.event.EscanearUiEvent
import com.cookingplastic.triskeledu.ui.viewmodels.EscanearViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EscanearViewModelTest {

    @Test
    fun soloSeTomaElPrimerCodigoLeido() {
        val viewModel = EscanearViewModel()
        viewModel.onEvent(EscanearUiEvent.OnCodigoLeido("LA-MONEDA"))
        viewModel.onEvent(EscanearUiEvent.OnCodigoLeido("OTRO-QR"))

        assertEquals("LA-MONEDA", viewModel.estado.value.codigoLeido)
    }

    @Test
    fun escanearOtroVuelveABuscar() {
        val viewModel = EscanearViewModel()
        viewModel.onEvent(EscanearUiEvent.OnCodigoLeido("LA-MONEDA"))
        viewModel.onEvent(EscanearUiEvent.OnEscanearOtro)

        assertNull(viewModel.estado.value.codigoLeido)
    }

    @Test
    fun guardaLaRespuestaDelPermiso() {
        val viewModel = EscanearViewModel()
        viewModel.onEvent(EscanearUiEvent.OnPermisoCamara(false))

        assertEquals(false, viewModel.estado.value.permisoCamara)
    }
}