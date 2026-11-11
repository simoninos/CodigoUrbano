package com.cookingplastic.triskeledu.ui.screens.mislugares

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.location.distanciaEnMetros
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.PUNTOS_MAXIMOS_LUGAR
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.estaCompletado
import com.cookingplastic.triskeledu.data.model.puntosTotales
import com.cookingplastic.triskeledu.domain.repository.ProgresoRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import com.cookingplastic.triskeledu.navigation.PedidoMapa
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

//Un lugar escaneado hace menos de un día se muestra como "Recién descubierto"
private const val UN_DIA_MS = 24 * 60 * 60 * 1000L

//Lo que eligió el usuario: filtro, orden y la vista abierta (P07-A a P07-D)
private data class Opciones(
    val filtro: FiltroLugares = FiltroLugares.TODOS,
    val orden: OrdenLugares = OrdenLugares.RECIENTES,
    val vista: VistaDescubrimientos = VistaDescubrimientos.LUGARES
)

//Recibe las fuentes de datos: el progreso (Catalina), el usuario conectado (Gabriel),
//la lista de lugares activos (obtenerActivos() de PuntoInteresRepository, de Simon) y las cápsulas
class MisLugaresViewModel(
    private val progresoRepository: ProgresoRepository,
    private val usuarioRepository: UsuarioRepository,
    private val puntosActivos: Flow<List<PuntoInteres>>,
    private val capsulas: Flow<List<Capsula>> = flowOf(emptyList()),
    //LUG-03: el pedido para abrir el mapa en un lugar
    private val pedidoMapa: PedidoMapa = PedidoMapa()
) : ViewModel() {

    private val _estado = MutableStateFlow(MisLugaresUiState())
    val estado: StateFlow<MisLugaresUiState> = _estado.asStateFlow()

    private val opciones = MutableStateFlow(Opciones())

    //cada vez que cambia el usuario conectado, se pasa a escuchar el progreso de ese usuario
    @OptIn(ExperimentalCoroutinesApi::class)
    private val progreso: Flow<List<ProgresoPunto>> =
        usuarioRepository.usuarioActual().flatMapLatest { usuario ->
            if (usuario == null) flowOf(emptyList()) else progresoRepository.progresoDe(usuario.id)
        }

    init {
        viewModelScope.launch {
            //combine junta las fuentes: si cualquiera cambia (por ejemplo, al escanear o ver una cápsula),
            //se vuelve a armar la pantalla al instante (LUG-01)
            combine(puntosActivos, progreso, capsulas, opciones) { puntos, filas, listaCapsulas, elegidas ->
                armarMisLugares(puntos, filas, elegidas.filtro, elegidas.orden, System.currentTimeMillis(), listaCapsulas, elegidas.vista)
            }
                .catch { error ->
                    _estado.update { it.copy(cargando = false, mensajeError = "No se pudieron cargar tus lugares") }
                }
                //irAlMapa es de la pantalla: no se pierde al recalcular
                .collect { nuevo -> _estado.update { nuevo.copy(irAlMapa = it.irAlMapa) } }
        }
    }

    fun onEvent(evento: MisLugaresUiEvent) {
        when (evento) {
            is MisLugaresUiEvent.OnFiltroChange -> opciones.update { it.copy(filtro = evento.filtro) }
            is MisLugaresUiEvent.OnOrdenChange -> opciones.update { it.copy(orden = evento.orden) }
            is MisLugaresUiEvent.OnVistaChange -> opciones.update { it.copy(vista = evento.vista) }
            is MisLugaresUiEvent.OnVerEnMapa -> {
                pedidoMapa.centrarEn(evento.puntoId)
                _estado.update { it.copy(irAlMapa = true) }
            }
            MisLugaresUiEvent.OnMapaAbierto -> _estado.update { it.copy(irAlMapa = false) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                MisLugaresViewModel(
                    progresoRepository = RepositoriosProgreso.progreso(app),
                    usuarioRepository = app.container.usuarioRepository,
                    puntosActivos = app.container.puntoInteresRepository.obtenerActivos(),
                    capsulas = app.container.capsulaRepository.capsulas(),
                    pedidoMapa = app.container.pedidoMapa
                )
            }
        }
    }
}

//Arma todo lo que muestra Mis descubrimientos a partir de los datos.
//No usa Android ni corrutinas: recibe listas y devuelve el estado, así se prueba fácil
fun armarMisLugares(
    puntos: List<PuntoInteres>,
    progreso: List<ProgresoPunto>,
    filtro: FiltroLugares,
    orden: OrdenLugares,
    ahora: Long,
    capsulas: List<Capsula> = emptyList(),
    vista: VistaDescubrimientos = VistaDescubrimientos.LUGARES
): MisLugaresUiState {
    //para buscar rápido la fila de progreso de cada lugar: puntoId -> fila
    val progresoPorPunto = progreso.associateBy { it.puntoId }
    val capsulaPorId = capsulas.associateBy { it.id }

    //los lugares activos que el usuario ya escaneó, con su fila; Y de "X de Y" son los activos (LUG-01)
    val escaneados = puntos.mapNotNull { punto -> progresoPorPunto[punto.id]?.let { punto to it } }

    //P07-A: solo los lugares que tienen fila de progreso están desbloqueados
    val desbloqueados = escaneados.map { (punto, fila) ->
        LugarDesbloqueado(
            puntoId = punto.id,
            nombre = punto.nombre,
            tipo = punto.tipo.descripcion,
            puntos = fila.puntosTotales(),
            maximo = PUNTOS_MAXIMOS_LUGAR,
            estado = when {
                fila.estaCompletado() -> EstadoLugar.COMPLETADO
                //primero la cápsula; recién escaneado (menos de un día) se muestra como recién descubierto
                fila.capsulaVistaEn == null && ahora - fila.escaneadoEn < UN_DIA_MS -> EstadoLugar.RECIEN_DESCUBIERTO
                fila.capsulaVistaEn == null -> EstadoLugar.FALTA_CAPSULA
                else -> EstadoLugar.FALTA_SELFIE
            },
            escaneadoEn = fila.escaneadoEn
        )
    }
    val completados = escaneados.filter { (_, fila) -> fila.estaCompletado() }.map { it.first.id }.toSet()
    val puntosDe = escaneados.associate { (punto, fila) -> punto.id to fila.puntosTotales() }

    //P07-B: solo las cápsulas de lugares escaneados (RN-23)
    val capsulasDescubiertas = escaneados.mapNotNull { (punto, fila) ->
        val capsula = punto.capsulaId?.let { capsulaPorId[it] } ?: return@mapNotNull null
        CapsulaDescubierta(
            puntoId = punto.id,
            capsulaId = capsula.id,
            titulo = capsula.titulo,
            categoria = capsula.categoria.etiqueta,
            nombrePunto = punto.nombre,
            vista = fila.capsulaVistaEn != null,
            fecha = fila.capsulaVistaEn ?: fila.escaneadoEn
        )
    }

    //P07-C: cada QR con la distancia a la que se escaneó
    val qrs = escaneados.map { (punto, fila) ->
        QrEscaneado(
            puntoId = punto.id,
            nombre = punto.nombre,
            escaneadoEn = fila.escaneadoEn,
            distanciaMetros = distanciaEnMetros(fila.escaneoUbicacion, punto.ubicacion).roundToInt()
        )
    }

    //P07-D: solo las selfies que ya tomó
    val selfies = escaneados.mapNotNull { (punto, fila) ->
        val tomadaEn = fila.selfieEn ?: return@mapNotNull null
        SelfieTomada(puntoId = punto.id, nombre = punto.nombre, selfieUri = fila.selfieUri, selfieEn = tomadaEn)
    }

    //LUG-03: lo que falta en la vista abierta. Las selfies cuentan los lugares sin selfie;
    //las demás, los lugares que todavía no escanea
    val porDescubrir = puntos
        .filter { punto ->
            val fila = progresoPorPunto[punto.id]
            if (vista == VistaDescubrimientos.SELFIES) fila?.selfieEn == null else fila == null
        }
        .map { LugarPorDescubrir(puntoId = it.id, nombre = it.nombre) }
        .sortedBy { it.nombre }

    return MisLugaresUiState(
        //los totales cuentan todo lo ganado, sin importar el filtro elegido
        totalPuntos = progreso.sumOf { it.puntosTotales() },
        lugaresEscaneados = escaneados.size,
        totalLugares = puntos.size,
        qrEscaneados = escaneados.size,
        capsulasVistas = escaneados.count { (_, fila) -> fila.capsulaVistaEn != null },
        selfiesTomadas = selfies.size,
        vista = vista,
        desbloqueados = desbloqueados
            .filter { lugar -> pasaFiltro(filtro, lugar.estado == EstadoLugar.COMPLETADO) }
            .ordenar(orden, { it.escaneadoEn }, { it.nombre }, { it.puntos }),
        capsulas = capsulasDescubiertas
            .filter { pasaFiltro(filtro, it.vista) }
            .ordenar(orden, { it.fecha }, { it.titulo }, { puntosDe[it.puntoId] ?: 0 }),
        qrs = qrs
            .filter { pasaFiltro(filtro, it.puntoId in completados) }
            .ordenar(orden, { it.escaneadoEn }, { it.nombre }, { puntosDe[it.puntoId] ?: 0 }),
        selfies = selfies
            .filter { pasaFiltro(filtro, it.puntoId in completados) }
            .ordenar(orden, { it.selfieEn }, { it.nombre }, { puntosDe[it.puntoId] ?: 0 }),
        porDescubrir = porDescubrir,
        filtro = filtro,
        orden = orden,
        cargando = false
    )
}

//"En curso" = lo que falta terminar; "Completados" = lo terminado
private fun pasaFiltro(filtro: FiltroLugares, terminado: Boolean): Boolean = when (filtro) {
    FiltroLugares.TODOS -> true
    FiltroLugares.EN_CURSO -> !terminado
    FiltroLugares.COMPLETADOS -> terminado
}

//el mismo orden para las cuatro listas: cada una dice cuál es su fecha, su nombre y sus puntos
private fun <T> List<T>.ordenar(orden: OrdenLugares, fecha: (T) -> Long, nombre: (T) -> String, puntos: (T) -> Int): List<T> =
    when (orden) {
        OrdenLugares.RECIENTES -> sortedByDescending(fecha)
        OrdenLugares.NOMBRE -> sortedBy(nombre)
        OrdenLugares.PUNTOS -> sortedByDescending(puntos)
    }
