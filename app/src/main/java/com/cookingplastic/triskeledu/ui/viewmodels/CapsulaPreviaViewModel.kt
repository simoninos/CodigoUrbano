package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.creador.GestorCapsulas
import com.cookingplastic.triskeledu.data.creador.ResultadoCapsula
import com.cookingplastic.triskeledu.data.repository.CapsulaRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.ui.event.CapsulaPreviaUiEvent
import com.cookingplastic.triskeledu.ui.state.CapsulaPreviaUiState
import com.cookingplastic.triskeledu.ui.validation.ValidacionCapsula
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//P18-B: decide si se puede publicar. Solo se publica un video que el reproductor alcanzó a cargar
//(así se sabe que el archivo es público) y que dura 3 minutos o menos (RN-26)
class CapsulaPreviaViewModel(
    private val capsulaId: String,
    capsulaRepository: CapsulaRepository,
    private val gestorCapsulas: GestorCapsulas
) : ViewModel() {

    private val _estado = MutableStateFlow(CapsulaPreviaUiState())
    val estado: StateFlow<CapsulaPreviaUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            capsulaRepository.observar(capsulaId).collect { capsula -> _estado.update { it.copy(estadoCapsula = capsula?.estado) } }
        }
    }

    fun onEvent(evento: CapsulaPreviaUiEvent) {
        when (evento) {
            is CapsulaPreviaUiEvent.OnPublicar -> publicar(evento.videoCargado, evento.duracionMs)
        }
    }

    private fun publicar(videoCargado: Boolean, duracionMs: Long) {
        val motivo = motivoNoPublicarVideo(videoCargado, duracionMs)
        if (motivo != null) {
            _estado.update { it.copy(mensajeError = motivo) }
            return
        }
        _estado.update { it.copy(publicando = true, mensajeError = null) }
        viewModelScope.launch {
            when (val resultado = gestorCapsulas.publicar(capsulaId)) {
                ResultadoCapsula.Listo -> _estado.update { it.copy(publicando = false, publicada = true) }
                is ResultadoCapsula.Error -> _estado.update { it.copy(publicando = false, mensajeError = resultado.mensaje) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                CapsulaPreviaViewModel(
                    capsulaId = createSavedStateHandle().get<String>(Rutas.ARG_CAPSULA_ID).orEmpty(),
                    capsulaRepository = app.container.capsulaRepository,
                    gestorCapsulas = app.container.gestorCapsulas
                )
            }
        }
    }
}

//CRE-03: null si el video sirve para publicar. Se da un segundo de margen por cómo redondea el reproductor
fun motivoNoPublicarVideo(videoCargado: Boolean, duracionMs: Long): String? = when {
    !videoCargado || duracionMs <= 0 -> "Espera a que el video cargue para confirmar que el enlace funciona"
    duracionMs > (ValidacionCapsula.DURACION_MAXIMA + 1) * 1000L -> "El video dura más de 3 minutos: acórtalo antes de publicarlo"
    else -> null
}
