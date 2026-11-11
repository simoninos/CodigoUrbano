package com.cookingplastic.triskeledu.ui.screens.creador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.creador.GestorCapsulas
import com.cookingplastic.triskeledu.data.creador.ResultadoCapsula
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.domain.repository.CapsulaRepository
import com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.domain.rules.ReglaCapsula
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//P21: el creador elige una de sus cápsulas para un punto Sin cápsula (CRE-08, RN-28)
class AsociarCapsulaViewModel(
    private val puntoId: String,
    usuarioRepository: UsuarioRepository,
    capsulaRepository: CapsulaRepository,
    puntoInteresRepository: PuntoInteresRepository,
    private val gestorCapsulas: GestorCapsulas
) : ViewModel() {

    private val _estado = MutableStateFlow(AsociarCapsulaUiState())
    val estado: StateFlow<AsociarCapsulaUiState> = _estado.asStateFlow()

    private var usuario: Usuario? = null

    init {
        viewModelScope.launch {
            combine(usuarioRepository.usuarioActual(), capsulaRepository.capsulas(), puntoInteresRepository.obtenerTodos()) { u, capsulas, puntos ->
                usuario = u
                armarAsociar(puntoId, u, capsulas, puntos)
            }.collect { nuevo ->
                //se conserva lo que eligió, si sigue siendo elegible
                _estado.update { actual ->
                    val elegida = actual.elegidaId?.takeIf { id -> nuevo.opciones.any { it.capsula.id == id && it.elegible } }
                    nuevo.copy(elegidaId = elegida, asociando = actual.asociando, asociada = actual.asociada, mensajeError = actual.mensajeError)
                }
            }
        }
    }

    fun onEvent(evento: AsociarCapsulaUiEvent) {
        when (evento) {
            //las deshabilitadas no se pueden elegir
            is AsociarCapsulaUiEvent.OnElegir ->
                if (_estado.value.opciones.any { it.capsula.id == evento.capsulaId && it.elegible }) {
                    _estado.update { it.copy(elegidaId = evento.capsulaId, mensajeError = null) }
                }
            AsociarCapsulaUiEvent.OnAsociar -> asociar()
        }
    }

    private fun asociar() {
        val actual = _estado.value
        val elegida = actual.elegidaId
        val quien = usuario
        if (!actual.puedeAsociar || elegida == null || quien == null) return
        _estado.update { it.copy(asociando = true, mensajeError = null) }
        viewModelScope.launch {
            when (val resultado = gestorCapsulas.asociar(elegida, puntoId, quien)) {
                ResultadoCapsula.Listo -> _estado.update { it.copy(asociando = false, asociada = true) }
                is ResultadoCapsula.Error -> _estado.update { it.copy(asociando = false, mensajeError = resultado.mensaje) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                AsociarCapsulaViewModel(
                    puntoId = createSavedStateHandle().get<String>(Rutas.ARG_PUNTO_ID).orEmpty(),
                    usuarioRepository = app.container.usuarioRepository,
                    capsulaRepository = app.container.capsulaRepository,
                    puntoInteresRepository = app.container.puntoInteresRepository,
                    gestorCapsulas = app.container.gestorCapsulas
                )
            }
        }
    }
}

//Arma P21: las cápsulas del usuario, primero las que se pueden elegir. El administrador ve las de todos (RN-28)
fun armarAsociar(puntoId: String, usuario: Usuario?, capsulas: List<Capsula>, puntos: List<PuntoInteres>): AsociarCapsulaUiState {
    val punto = puntos.find { it.id == puntoId }
    if (usuario == null || punto == null) {
        return AsociarCapsulaUiState(punto = punto, motivoPunto = if (punto == null) "Este punto ya no existe" else null, cargando = false)
    }
    val capsulaDelPunto = punto.capsulaId?.let { id -> capsulas.find { it.id == id } }
    val visibles = capsulas.filter { usuario.rol == RolUsuario.ADMINISTRADOR || it.creadorId == usuario.id }
    val opciones = visibles
        .map { OpcionCapsula(capsula = it, motivo = ReglaCapsula.motivoNoAsociable(it, usuario, puntos)) }
        .sortedWith(compareBy<OpcionCapsula> { !it.elegible }.thenBy { it.capsula.titulo })
    return AsociarCapsulaUiState(
        punto = punto,
        motivoPunto = ReglaCapsula.motivoPuntoNoAsociable(punto, capsulaDelPunto),
        opciones = opciones,
        cargando = false
    )
}
