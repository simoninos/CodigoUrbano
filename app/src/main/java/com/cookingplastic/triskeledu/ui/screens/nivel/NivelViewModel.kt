package com.cookingplastic.triskeledu.ui.screens.nivel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.domain.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.domain.repository.ProgresoRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import com.cookingplastic.triskeledu.domain.rules.ReglaNivel
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

//P24 · Mi nivel: el nivel sale de los puntos totales y se actualiza solo al sumar o perder puntos (EXC-12)
class NivelViewModel(
    private val progresoRepository: ProgresoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val preferenciasRepository: PreferenciasRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(NivelUiState())
    val estado: StateFlow<NivelUiState> = _estado.asStateFlow()

    init {
        //el nombre y los puntos del usuario conectado; sin sesión, 0 puntos
        @OptIn(ExperimentalCoroutinesApi::class)
        val usuarioYPuntos = usuarioRepository.usuarioActual().flatMapLatest { usuario ->
            if (usuario == null) flowOf("" to 0)
            else progresoRepository.totalPuntos(usuario.id).map { usuario.username to it }
        }
        viewModelScope.launch {
            combine(usuarioYPuntos, preferenciasRepository.preferencias()) { (nombre, puntos), preferencias ->
                armarNivel(puntos, preferencias.nivelMaximoAlcanzado).copy(nombreUsuario = nombre)
            }.collect { nuevo -> _estado.value = nuevo }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                NivelViewModel(
                    RepositoriosProgreso.progreso(app),
                    app.container.usuarioRepository,
                    app.container.preferenciasRepository
                )
            }
        }
    }
}

//Arma P24 desde los puntos. Los niveles por debajo del actual, y los que ya alcanzó antes aunque haya
//bajado, quedan desbloqueados porque su tema se conserva (RN-33)
fun armarNivel(puntos: Int, nivelMaximoAlcanzado: Int): NivelUiState {
    val nivel = ReglaNivel.nivelDesde(puntos)
    val filas = TemaApp.entries.map { tema ->
        val numero = tema.nivelRequerido
        val umbral = ReglaNivel.umbral(numero)
        FilaNivel(
            numero = numero,
            nombre = ReglaNivel.nombre(numero),
            umbral = umbral,
            tema = tema,
            estado = when {
                numero == nivel.numero -> EstadoFilaNivel.NIVEL_ACTUAL
                numero <= maxOf(nivel.numero, nivelMaximoAlcanzado) -> EstadoFilaNivel.DESBLOQUEADO
                else -> EstadoFilaNivel.BLOQUEADO
            },
            faltanPuntos = (umbral - puntos).coerceAtLeast(0)
        )
    }
    return NivelUiState(nivel = nivel, filas = filas, cargando = false)
}
