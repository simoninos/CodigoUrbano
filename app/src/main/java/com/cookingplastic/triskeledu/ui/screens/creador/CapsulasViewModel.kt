package com.cookingplastic.triskeledu.ui.screens.creador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.CategoriaCapsula
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.domain.repository.CapsulaRepository
import com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

//Lo que eligió el creador en la búsqueda y los filtros
private data class OpcionesCapsulas(
    val busqueda: String = "",
    val filtro: FiltroEstadoCapsula = FiltroEstadoCapsula.TODAS,
    val categoria: CategoriaCapsula? = null
)

//P17 · Mis cápsulas (CRE-04): solo las del creador conectado, con el punto de cada una
class CapsulasViewModel(
    usuarioRepository: UsuarioRepository,
    capsulaRepository: CapsulaRepository,
    puntoInteresRepository: PuntoInteresRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(CapsulasUiState())
    val estado: StateFlow<CapsulasUiState> = _estado.asStateFlow()

    private val opciones = MutableStateFlow(OpcionesCapsulas())

    init {
        viewModelScope.launch {
            combine(
                usuarioRepository.usuarioActual(),
                capsulaRepository.capsulas(),
                puntoInteresRepository.obtenerTodos(),
                opciones
            ) { usuario, capsulas, puntos, elegidas ->
                val propias = capsulas.filter { it.creadorId == usuario?.id }
                armarCapsulas(propias, puntos, elegidas.busqueda, elegidas.filtro, elegidas.categoria)
            }.collect { nuevo -> _estado.value = nuevo }
        }
    }

    fun onEvent(evento: CapsulasUiEvent) {
        when (evento) {
            is CapsulasUiEvent.OnBusquedaChange -> opciones.update { it.copy(busqueda = evento.texto) }
            is CapsulasUiEvent.OnFiltroEstadoChange -> opciones.update { it.copy(filtro = evento.filtro) }
            is CapsulasUiEvent.OnCategoriaChange -> opciones.update { it.copy(categoria = evento.categoria) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                CapsulasViewModel(app.container.usuarioRepository, app.container.capsulaRepository, app.container.puntoInteresRepository)
            }
        }
    }
}

//Filtra y ordena las cápsulas del creador: primero los borradores, después por título.
//La búsqueda no distingue mayúsculas ni tildes ("introduccion" encuentra "Introducción")
fun armarCapsulas(
    propias: List<Capsula>,
    puntos: List<PuntoInteres>,
    busqueda: String,
    filtro: FiltroEstadoCapsula,
    categoria: CategoriaCapsula?
): CapsulasUiState {
    val puntoPorCapsula = puntos.filter { it.capsulaId != null }.associateBy { it.capsulaId }
    val texto = sinTildes(busqueda.trim())
    val filas = propias
        .filter { texto.isEmpty() || sinTildes(it.titulo).contains(texto) }
        .filter {
            when (filtro) {
                FiltroEstadoCapsula.TODAS -> true
                FiltroEstadoCapsula.BORRADORES -> it.estado == EstadoCapsula.BORRADOR
                FiltroEstadoCapsula.PUBLICADAS -> it.estado == EstadoCapsula.PUBLICADA
            }
        }
        .filter { categoria == null || it.categoria == categoria }
        .sortedWith(compareBy<Capsula> { it.estado != EstadoCapsula.BORRADOR }.thenBy { it.titulo })
        .map { FilaCapsula(capsula = it, nombrePunto = puntoPorCapsula[it.id]?.nombre) }
    return CapsulasUiState(
        busqueda = busqueda,
        filtroEstado = filtro,
        categoria = categoria,
        capsulas = filas,
        totalPropias = propias.size,
        cargando = false
    )
}

private fun sinTildes(texto: String): String =
    Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
