package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosCompartidos
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.location.UbicacionRepository
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.model.puntosTotales
import com.cookingplastic.triskeledu.data.red.AndroidRedRepository
import com.cookingplastic.triskeledu.data.red.RedRepository
import com.cookingplastic.triskeledu.data.repository.CapsulaRepository
import com.cookingplastic.triskeledu.data.repository.ComentarioRepository
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.ui.event.DetalleUiEvent
import com.cookingplastic.triskeledu.ui.state.DetalleUiState
import com.cookingplastic.triskeledu.ui.validation.ValidacionComentario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel de la pantalla 4 (Detalle del punto).
// El id del punto llega en la ruta "detalle/{puntoId}" y se lee desde SavedStateHandle.
// El estado del punto y su avance salen del progreso del usuario conectado en ese punto.
// Los comentarios del lugar se leen y se guardan con ComentarioRepository.
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleViewModel(
    savedStateHandle: SavedStateHandle,
    private val puntoInteresRepository: PuntoInteresRepository,
    private val ubicacionRepository: UbicacionRepository,
    private val redRepository: RedRepository,
    usuarioRepository: UsuarioRepository,
    progresoRepository: ProgresoRepository,
    private val comentarioRepository: ComentarioRepository,
    capsulaRepository: CapsulaRepository
) : ViewModel() {

    private val puntoId: String = savedStateHandle.get<String>(Rutas.ARG_PUNTO_ID).orEmpty()

    private val _estado = MutableStateFlow(DetalleUiState())
    val estado: StateFlow<DetalleUiState> = _estado.asStateFlow()

    // Distancia en vivo; sin permiso o sin GPS se sigue sin distancia (RNF-11)
    private val ubicacion: Flow<Ubicacion?> = ubicacionRepository.ubicaciones()
        .map<Ubicacion, Ubicacion?> { it }
        .onStart { emit(ubicacionRepository.ultimaUbicacion()) }
        .catch { emit(null) }

    // Fila de progreso del usuario en este punto; null = aún no lo escanea (bloqueado).
    // Al cambiar de usuario, flatMapLatest pasa a escuchar la fila del nuevo.
    // D-21: el punto junto con su cápsula. combine acepta hasta cinco fuentes por separado,
    // por eso el punto y las cápsulas se juntan aquí antes de entrar al combine principal.
    private val puntoConCapsula: Flow<Pair<PuntoInteres?, Capsula?>> =
        combine(puntoInteresRepository.obtenerPorId(puntoId), capsulaRepository.capsulas()) { punto, capsulas ->
            punto to capsulas.find { it.id == punto?.capsulaId }
        }

    private val usuario: Flow<Usuario?> = usuarioRepository.usuarioActual()

    private val progreso: Flow<ProgresoPunto?> = usuario.flatMapLatest { conectado ->
        if (conectado == null) flowOf(null) else progresoRepository.progresoDePunto(conectado.id, puntoId)
    }

    init {
        viewModelScope.launch {
            combine(
                puntoConCapsula,
                ubicacion,
                usuario,
                progreso,
                comentarioRepository.comentariosDe(puntoId)
            ) { (punto, capsula), ubicacionActual, usuarioActual, fila, comentarios ->
                _estado.update {
                    it.copy(
                        punto = punto,
                        capsula = capsula,
                        usuarioId = usuarioActual?.id,
                        comentarios = comentarios,
                        estadoPunto = EstadoPunto.desde(fila),
                        puntosLugar = fila?.puntosTotales() ?: 0,
                        videoVisto = fila?.videoVistoEn != null,
                        distanciaMetros = if (punto != null && ubicacionActual != null) {
                            ubicacionRepository.distanciaMetros(ubicacionActual, punto.ubicacion)
                        } else {
                            it.distanciaMetros
                        },
                        cargando = false,
                        // D-01
                        noEncontrado = punto == null
                    )
                }
            }
                .catch { _estado.update { it.copy(cargando = false, mensaje = "No se pudo cargar el lugar") } }
                .collect { }
        }
    }

    fun onEvent(evento: DetalleUiEvent) {
        when (evento) {
            DetalleUiEvent.OnVerVideo -> verVideo()
            DetalleUiEvent.OnTomarSelfie -> actividadDesbloqueable()
            DetalleUiEvent.OnComentar -> alternarFormularioComentario()
            is DetalleUiEvent.OnTextoComentarioChange -> _estado.update {
                // D-17: se valida en cada cambio, así el error aparece y desaparece mientras se escribe
                it.copy(textoComentario = evento.texto, errorComentario = ValidacionComentario.validar(evento.texto))
            }
            DetalleUiEvent.OnPublicarComentario -> publicarComentario()
            DetalleUiEvent.OnCerrarSinConexion -> _estado.update { it.copy(mostrarSinConexion = false) }
            DetalleUiEvent.OnMensajeMostrado -> _estado.update { it.copy(mensaje = null) }
            DetalleUiEvent.OnEnlaceNoDisponible -> _estado.update { it.copy(mensaje = "No se pudo abrir el enlace") }
        }
    }

    private fun verVideo() {
        // D-10: el video es streaming; sin internet se muestra el popup, sea cual sea el estado del punto
        if (!redRepository.hayConexion()) {
            _estado.update { it.copy(mostrarSinConexion = true) }
            return
        }
        actividadDesbloqueable()
    }

    // D-11 y D-16: comentar requiere el punto desbloqueado; si lo está, el formulario se abre o se cierra
    private fun alternarFormularioComentario() {
        if (!_estado.value.desbloqueado) {
            _estado.update { it.copy(mensaje = "Escanea el QR del lugar para desbloquear esta actividad") }
            return
        }
        _estado.update { it.copy(formularioComentarioAbierto = !it.formularioComentarioAbierto) }
    }

    // D-18 y D-19: se valida de nuevo antes de guardar; al terminar se limpia y se cierra el formulario
    private fun publicarComentario() {
        val actual = _estado.value
        val error = ValidacionComentario.validar(actual.textoComentario)
        if (error != null) {
            _estado.update { it.copy(errorComentario = error) }
            return
        }
        val usuarioId = actual.usuarioId
        if (usuarioId == null || !actual.desbloqueado || actual.guardandoComentario) return

        _estado.update { it.copy(guardandoComentario = true) }
        viewModelScope.launch {
            try {
                comentarioRepository.agregar(
                    Comentario(usuarioId = usuarioId, puntoId = puntoId, texto = actual.textoComentario.trim())
                )
                _estado.update {
                    it.copy(
                        textoComentario = "",
                        errorComentario = null,
                        formularioComentarioAbierto = false,
                        guardandoComentario = false,
                        mensaje = "Comentario guardado"
                    )
                }
            } catch (e: Exception) {
                // El texto se mantiene para que el usuario pueda intentarlo de nuevo
                _estado.update { it.copy(guardandoComentario = false, mensaje = "No se pudo guardar el comentario") }
            }
        }
    }

    // D-11: video, selfie y comentario se desbloquean al escanear el QR del punto (RN-07)
    private fun actividadDesbloqueable() {
        val mensaje = if (_estado.value.desbloqueado) {
            "Esta actividad estará disponible en la próxima versión"
        } else {
            "Escanea el QR del lugar para desbloquear esta actividad"
        }
        _estado.update { it.copy(mensaje = mensaje) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                DetalleViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    puntoInteresRepository = RepositoriosCompartidos.puntoInteres,
                    ubicacionRepository = RepositoriosCompartidos.ubicacion(app),
                    redRepository = AndroidRedRepository(app),
                    usuarioRepository = app.container.usuarioRepository,
                    progresoRepository = RepositoriosProgreso.progreso(app),
                    comentarioRepository = RepositoriosProgreso.comentarios(app),
                    capsulaRepository = app.container.capsulaRepository
                )
            }
        }
    }
}
