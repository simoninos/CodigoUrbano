package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.creador.GestorCapsulas
import com.cookingplastic.triskeledu.data.creador.ResultadoCapsula
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.repository.CapsulaRepository
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.ui.event.CapsulaDetalleUiEvent
import com.cookingplastic.triskeledu.ui.state.CapsulaDetalleUiState
import com.cookingplastic.triskeledu.ui.validation.ReglaCapsula
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//P17-B: la cápsula publicada, su punto y cuántas veces la vieron completa. Se actualiza solo si el
//administrador desactiva el punto mientras está abierta
class CapsulaDetalleViewModel(
    private val capsulaId: String,
    capsulaRepository: CapsulaRepository,
    puntoInteresRepository: PuntoInteresRepository,
    progresoRepository: ProgresoRepository,
    private val gestorCapsulas: GestorCapsulas
) : ViewModel() {

    private val _estado = MutableStateFlow(CapsulaDetalleUiState())
    val estado: StateFlow<CapsulaDetalleUiState> = _estado.asStateFlow()

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        val datos = combine(capsulaRepository.observar(capsulaId), puntoInteresRepository.obtenerTodos()) { capsula, puntos ->
            capsula to capsula?.let { ReglaCapsula.puntoDe(it, puntos) }
        }.flatMapLatest { (capsula, punto) ->
            //las vistas completas se cuentan en el punto de la cápsula
            val vistas = if (punto == null) flowOf(0) else progresoRepository.vistasCompletas(punto.id)
            vistas.map { Triple(capsula, punto, it) }
        }
        viewModelScope.launch {
            datos.collect { (capsula, punto, vistas) ->
                _estado.update {
                    it.copy(
                        capsula = capsula,
                        punto = punto,
                        estadoPunto = punto?.let { p -> EstadoAdminPunto.desde(p, capsula) },
                        vistasCompletas = vistas,
                        motivoNoRetirable = capsula?.let { c -> ReglaCapsula.motivoNoRetirable(c, punto) },
                        noExiste = capsula == null,
                        cargando = false
                    )
                }
            }
        }
    }

    fun onEvent(evento: CapsulaDetalleUiEvent) {
        when (evento) {
            //bloqueado con punto activo: el botón no abre el diálogo (RN-21)
            CapsulaDetalleUiEvent.OnRetirar ->
                if (_estado.value.motivoNoRetirable == null) _estado.update { it.copy(confirmarRetiro = true, mensajeError = null) }
            CapsulaDetalleUiEvent.OnCancelarRetiro -> _estado.update { it.copy(confirmarRetiro = false) }
            CapsulaDetalleUiEvent.OnConfirmarRetiro -> retirar()
        }
    }

    private fun retirar() {
        _estado.update { it.copy(confirmarRetiro = false, retirando = true) }
        viewModelScope.launch {
            when (val resultado = gestorCapsulas.retirar(capsulaId)) {
                ResultadoCapsula.Listo -> _estado.update { it.copy(retirando = false, retirada = true) }
                is ResultadoCapsula.Error -> _estado.update { it.copy(retirando = false, mensajeError = resultado.mensaje) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                CapsulaDetalleViewModel(
                    capsulaId = createSavedStateHandle().get<String>(Rutas.ARG_CAPSULA_ID).orEmpty(),
                    capsulaRepository = app.container.capsulaRepository,
                    puntoInteresRepository = app.container.puntoInteresRepository,
                    progresoRepository = RepositoriosProgreso.progreso(app),
                    gestorCapsulas = app.container.gestorCapsulas
                )
            }
        }
    }
}
