package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.model.puntosTotales
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.event.RankingUiEvent
import com.cookingplastic.triskeledu.ui.state.FilaRanking
import com.cookingplastic.triskeledu.ui.state.PeriodoRanking
import com.cookingplastic.triskeledu.ui.state.RankingUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

//P20 · Ranking (LUG-06): los usuarios del teléfono ordenados por puntos, este mes o desde siempre.
//Se recalcula solo cuando alguien suma o pierde puntos
class RankingViewModel(
    private val progresoRepository: ProgresoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val ahora: () -> Long = System::currentTimeMillis,
    private val zona: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _estado = MutableStateFlow(RankingUiState())
    val estado: StateFlow<RankingUiState> = _estado.asStateFlow()

    private val periodo = MutableStateFlow(PeriodoRanking.ESTE_MES)

    init {
        viewModelScope.launch {
            try {
                //la lista de usuarios casi no cambia: se lee una vez al abrir
                val usuarios = usuarioRepository.todos()
                combine(progresoRepository.todas(), usuarioRepository.usuarioActual(), periodo) { filas, actual, elegido ->
                    val (primeros, filaActual) = armarRanking(usuarios, filas, actual?.id, elegido, ahora(), zona)
                    RankingUiState(periodo = elegido, primeros = primeros, filaActual = filaActual, cargando = false)
                }.collect { _estado.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                //RNF-11: si no se pudo leer, la pantalla queda vacía con su mensaje en vez de cerrar la app
                _estado.update { it.copy(cargando = false) }
            }
        }
    }

    fun onEvent(evento: RankingUiEvent) {
        when (evento) {
            is RankingUiEvent.OnPeriodoChange -> periodo.value = evento.periodo
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                RankingViewModel(RepositoriosProgreso.progreso(app), app.container.usuarioRepository)
            }
        }
    }
}

//cuántos aparecen arriba antes de "···" (como en el mockup)
const val PRIMEROS_RANKING = 5

//Arma P20: los primeros y, si el usuario conectado no está entre ellos, su fila aparte.
//El administrador no juega y una cuenta en revisión todavía no entra, así que no aparecen.
//Con empate comparten posición (1, 2, 2, 4) y se ordenan por nombre
fun armarRanking(
    usuarios: List<Usuario>,
    progreso: List<ProgresoPunto>,
    usuarioActualId: String?,
    periodo: PeriodoRanking,
    ahora: Long,
    zona: ZoneId = ZoneId.systemDefault(),
    cantidad: Int = PRIMEROS_RANKING
): Pair<List<FilaRanking>, FilaRanking?> {
    val desde = when (periodo) {
        PeriodoRanking.SIEMPRE -> Long.MIN_VALUE
        //desde el día 1 del mes actual a las 00:00
        PeriodoRanking.ESTE_MES -> Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate().withDayOfMonth(1)
            .atStartOfDay(zona).toInstant().toEpochMilli()
    }
    val porUsuario = progreso.groupBy { it.usuarioId }
    val jugadores = usuarios.filter { it.rol != RolUsuario.ADMINISTRADOR && it.solicitudCreador != EstadoSolicitud.PENDIENTE }
    val puntos = jugadores.associate { usuario ->
        usuario.id to (porUsuario[usuario.id].orEmpty().sumOf { if (periodo == PeriodoRanking.SIEMPRE) it.puntosTotales() else puntosDesde(it, desde) })
    }
    val ordenados = jugadores.sortedWith(compareByDescending<Usuario> { puntos.getValue(it.id) }.thenBy { it.username.lowercase() })
    val filas = ordenados.map { usuario ->
        val suyos = puntos.getValue(usuario.id)
        FilaRanking(
            posicion = 1 + puntos.values.count { it > suyos },
            usuarioId = usuario.id,
            username = usuario.username,
            puntos = suyos,
            esActual = usuario.id == usuarioActualId
        )
    }
    val primeros = filas.take(cantidad)
    val filaActual = filas.drop(cantidad).find { it.esActual }
    return primeros to filaActual
}

//los puntos de un lugar que se ganaron desde esa fecha: cada actividad cuenta en el mes en que se hizo
private fun puntosDesde(fila: ProgresoPunto, desde: Long): Int =
    (if (fila.escaneadoEn >= desde) fila.puntosEscaneo else 0) +
        (if ((fila.capsulaVistaEn ?: Long.MIN_VALUE) >= desde) fila.puntosCapsula else 0) +
        (if ((fila.selfieEn ?: Long.MIN_VALUE) >= desde) fila.puntosSelfie else 0)
