package com.cookingplastic.triskeledu.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.model.PUNTOS_CAPSULA
import com.cookingplastic.triskeledu.data.model.tieneEnlace
import com.cookingplastic.triskeledu.data.red.AndroidRedRepository
import com.cookingplastic.triskeledu.data.red.RedRepository
import com.cookingplastic.triskeledu.domain.repository.CapsulaRepository
import com.cookingplastic.triskeledu.domain.repository.ProgresoRepository
import com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.domain.rules.EnlaceVideo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//P12: reproductor de una cápsula. No tiene el ExoPlayer (ese vive en la pantalla): decide el modo,
//guarda dónde va el video y suma los 50 puntos al terminar. Así se prueba sin Android
class VideoViewModel(
    private val capsulaId: String,
    private val puntoId: String?,
    private val esVistaPrevia: Boolean,
    private val capsulaRepository: CapsulaRepository,
    private val puntoInteresRepository: PuntoInteresRepository,
    private val usuarioRepository: UsuarioRepository,
    private val progresoRepository: ProgresoRepository,
    private val redRepository: RedRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(VideoUiState(esVistaPrevia = esVistaPrevia))
    val estado: StateFlow<VideoUiState> = _estado.asStateFlow()

    private var usuarioId: String? = null
    //evita registrar dos veces si el reproductor avisa el final más de una vez
    private var registrando = false

    init {
        viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        val capsula = capsulaRepository.obtenerPorId(capsulaId)
        if (capsula == null) {
            _estado.update { it.copy(cargando = false, error = NO_DISPONIBLE) }
            return
        }
        val punto = puntoId?.let { puntoInteresRepository.obtenerPorId(it).first() }
        val enlace = capsula.urlStream?.takeIf { capsula.tieneEnlace() }?.let { EnlaceVideo.aReproducible(it) }
        _estado.update {
            it.copy(
                capsula = capsula,
                nombrePunto = punto?.nombre,
                enlace = enlace,
                sinEnlace = enlace == null
            )
        }

        //P18-B: el creador revisa su video; siempre libre y sin puntos
        if (esVistaPrevia) {
            _estado.update { it.copy(modo = ModoReproductor.LIBRE, cargando = false, sinConexion = faltaInternet(enlace)) }
            return
        }

        val usuario = usuarioRepository.usuarioActual().first()
        val fila = if (usuario != null && puntoId != null) progresoRepository.progresoDePunto(usuario.id, puntoId).first() else null
        //RN-07: la cápsula se desbloquea al escanear el QR de su punto
        if (usuario == null || puntoId == null || fila == null) {
            _estado.update { it.copy(cargando = false, error = "Escanea el QR del lugar para ver su cápsula") }
            return
        }
        usuarioId = usuario.id

        //RN-35: sin enlace no hay video; se simula la vista completa y suma 50 una sola vez
        if (enlace == null) {
            val sumo = progresoRepository.registrarVistaCapsula(usuario.id, puntoId)
            _estado.update {
                it.copy(
                    modo = ModoReproductor.LIBRE,
                    yaVista = !sumo,
                    puntosGanados = if (sumo) PUNTOS_CAPSULA else 0,
                    terminada = true,
                    cargando = false
                )
            }
            return
        }

        //RN-08: el modo lo decide el progreso, no la pantalla desde donde se abrió
        val yaVista = fila.capsulaVistaEn != null
        _estado.update {
            it.copy(
                modo = if (yaVista) ModoReproductor.LIBRE else ModoReproductor.BLOQUEADO,
                yaVista = yaVista,
                sinConexion = faltaInternet(enlace),
                cargando = false
            )
        }
    }

    fun onEvent(evento: VideoUiEvent) {
        when (evento) {
            is VideoUiEvent.OnAvance -> _estado.update { it.copy(posicionMs = evento.posicionMs, duracionMs = evento.duracionMs) }
            is VideoUiEvent.OnReproduciendo -> _estado.update { it.copy(reproduciendo = evento.valor) }
            VideoUiEvent.OnTerminada -> terminar()
            is VideoUiEvent.OnErrorReproduccion -> {
                //si se cortó internet se puede reintentar; si el archivo falla, no
                val porRed = evento.esDeRed || !redRepository.hayConexion()
                _estado.update {
                    if (porRed) it.copy(sinConexion = true, reproduciendo = false)
                    //CRE-03: en la vista previa el creador necesita saber qué revisar
                    else it.copy(error = if (it.esVistaPrevia) NO_REPRODUCE_PREVIA else NO_DISPONIBLE, reproduciendo = false)
                }
            }
            VideoUiEvent.OnReintentar -> {
                //sigue sin red: el aviso se queda
                if (!redRepository.hayConexion()) return
                _estado.update { it.copy(sinConexion = false, error = null, reproduciendo = true, intento = it.intento + 1) }
            }
            //ACT-06: se puede ver de nuevo, ya en modo libre y sin sumar
            VideoUiEvent.OnVerDeNuevo -> _estado.update {
                it.copy(terminada = false, posicionMs = 0, reproduciendo = true, intento = it.intento + 1)
            }
        }
    }

    private fun terminar() {
        val actual = _estado.value
        if (actual.terminada) return
        _estado.update { it.copy(terminada = true, reproduciendo = false, posicionMs = it.duracionMs) }
        //solo suma quien la vio completa en modo bloqueado; en libre o en la vista previa no hay puntos
        val usuario = usuarioId
        if (actual.esVistaPrevia || actual.modo != ModoReproductor.BLOQUEADO || usuario == null || puntoId == null || registrando) return
        registrando = true
        viewModelScope.launch {
            val sumo = progresoRepository.registrarVistaCapsula(usuario, puntoId)
            //desde ahora la cápsula ya está vista: si la ve de nuevo, va libre
            _estado.update { it.copy(puntosGanados = if (sumo) PUNTOS_CAPSULA else 0, yaVista = true, modo = ModoReproductor.LIBRE) }
            registrando = false
        }
    }

    //ACT-02: solo los videos de Drive necesitan internet
    private fun faltaInternet(enlace: String?): Boolean =
        enlace != null && EnlaceVideo.necesitaInternet(enlace) && !redRepository.hayConexion()

    companion object {
        const val NO_DISPONIBLE = "Esta cápsula no está disponible por ahora"
        const val NO_REPRODUCE_PREVIA = "No se pudo reproducir: revisa que el archivo sea público"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                //la ruta video/{capsulaId}?puntoId=...&vistaPrevia=... deja sus datos en el SavedStateHandle
                val argumentos = createSavedStateHandle()
                VideoViewModel(
                    capsulaId = argumentos.get<String>(Rutas.ARG_CAPSULA_ID).orEmpty(),
                    puntoId = argumentos.get<String>(Rutas.ARG_PUNTO_ID),
                    esVistaPrevia = argumentos.get<Boolean>(Rutas.ARG_VISTA_PREVIA) ?: false,
                    capsulaRepository = app.container.capsulaRepository,
                    puntoInteresRepository = app.container.puntoInteresRepository,
                    usuarioRepository = app.container.usuarioRepository,
                    progresoRepository = RepositoriosProgreso.progreso(app),
                    redRepository = AndroidRedRepository(app)
                )
            }
        }
    }
}
