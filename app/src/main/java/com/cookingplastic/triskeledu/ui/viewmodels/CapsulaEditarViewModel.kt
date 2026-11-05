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
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.repository.CapsulaRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.ui.event.CapsulaEditarUiEvent
import com.cookingplastic.triskeledu.ui.state.CampoCapsula
import com.cookingplastic.triskeledu.ui.state.CapsulaEditarUiState
import com.cookingplastic.triskeledu.ui.state.ConfirmacionCapsula
import com.cookingplastic.triskeledu.ui.validation.ValidacionCapsula
import com.cookingplastic.triskeledu.ui.validation.primerCampoConError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

//P18: crea una cápsula como BORRADOR o edita una existente. Las reglas de cada campo están en ValidacionCapsula
class CapsulaEditarViewModel(
    capsulaId: String?,
    private val usuarioRepository: UsuarioRepository,
    private val capsulaRepository: CapsulaRepository,
    private val gestorCapsulas: GestorCapsulas,
    private val ahora: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _estado = MutableStateFlow(CapsulaEditarUiState(capsulaId = capsulaId, cargando = capsulaId != null))
    val estado: StateFlow<CapsulaEditarUiState> = _estado.asStateFlow()

    //la cápsula como estaba guardada: conserva su creador y sus fechas al editarla
    private var original: Capsula? = null

    init {
        if (capsulaId != null) viewModelScope.launch { cargar(capsulaId) }
    }

    private suspend fun cargar(id: String) {
        val capsula = capsulaRepository.obtenerPorId(id)
        if (capsula == null) {
            _estado.update { it.copy(cargando = false, mensajeError = "Esta cápsula ya no existe") }
            return
        }
        original = capsula
        _estado.update {
            it.copy(
                estadoCapsula = capsula.estado,
                titulo = capsula.titulo,
                descripcion = capsula.descripcion.orEmpty(),
                categoria = capsula.categoria,
                enlace = capsula.urlStream.orEmpty(),
                duracion = capsula.duracionSegundos.toString(),
                cargando = false
            )
        }
    }

    fun onEvent(evento: CapsulaEditarUiEvent) {
        when (evento) {
            //si el campo ya mostraba un error, se revisa de nuevo al escribir: así desaparece apenas se corrige
            is CapsulaEditarUiEvent.OnTituloChange -> _estado.update {
                it.copy(titulo = evento.valor, errorTitulo = it.errorTitulo?.let { ValidacionCapsula.validarTitulo(evento.valor) })
            }
            is CapsulaEditarUiEvent.OnDescripcionChange -> _estado.update {
                it.copy(descripcion = evento.valor, errorDescripcion = ValidacionCapsula.validarDescripcion(evento.valor))
            }
            is CapsulaEditarUiEvent.OnCategoriaChange -> _estado.update { it.copy(categoria = evento.valor, errorCategoria = null) }
            is CapsulaEditarUiEvent.OnEnlaceChange -> _estado.update {
                it.copy(enlace = evento.valor, errorEnlace = it.errorEnlace?.let { ValidacionCapsula.validarEnlace(evento.valor) })
            }
            is CapsulaEditarUiEvent.OnDuracionChange -> _estado.update {
                //solo números
                val limpio = evento.valor.filter { c -> c.isDigit() }.take(4)
                it.copy(duracion = limpio, errorDuracion = it.errorDuracion?.let { ValidacionCapsula.validarDuracion(limpio) })
            }
            CapsulaEditarUiEvent.OnGuardar -> if (validar()) {
                //CRE-06: una cápsula que ya existía pide confirmación antes de cambiarla
                if (_estado.value.esNueva) guardar(abrirVistaPrevia = false)
                else _estado.update { it.copy(confirmacion = ConfirmacionCapsula.GUARDAR) }
            }
            CapsulaEditarUiEvent.OnProbarVideo -> if (validar()) guardar(abrirVistaPrevia = true)
            CapsulaEditarUiEvent.OnEliminar -> if (_estado.value.puedeEliminar) _estado.update { it.copy(confirmacion = ConfirmacionCapsula.ELIMINAR) }
            CapsulaEditarUiEvent.OnConfirmar -> when (_estado.value.confirmacion) {
                ConfirmacionCapsula.GUARDAR -> guardar(abrirVistaPrevia = false)
                ConfirmacionCapsula.ELIMINAR -> eliminar()
                null -> Unit
            }
            CapsulaEditarUiEvent.OnCancelarConfirmacion -> _estado.update { it.copy(confirmacion = null) }
            CapsulaEditarUiEvent.OnVistaPreviaAbierta -> _estado.update { it.copy(abrirVistaPrevia = null) }
        }
    }

    //RN-18: muestra todos los errores y lleva el foco al primero. Devuelve true si no hay ninguno
    private fun validar(): Boolean {
        val actual = _estado.value
        val errorTitulo = ValidacionCapsula.validarTitulo(actual.titulo)
        val errorDescripcion = ValidacionCapsula.validarDescripcion(actual.descripcion)
        val errorCategoria = ValidacionCapsula.validarCategoria(actual.categoria)
        val errorEnlace = ValidacionCapsula.validarEnlace(actual.enlace)
        val errorDuracion = ValidacionCapsula.validarDuracion(actual.duracion)
        val campo = primerCampoConError(
            CampoCapsula.TITULO to errorTitulo,
            CampoCapsula.DESCRIPCION to errorDescripcion,
            CampoCapsula.CATEGORIA to errorCategoria,
            CampoCapsula.ENLACE to errorEnlace,
            CampoCapsula.DURACION to errorDuracion
        )
        _estado.update {
            it.copy(
                errorTitulo = errorTitulo,
                errorDescripcion = errorDescripcion,
                errorCategoria = errorCategoria,
                errorEnlace = errorEnlace,
                errorDuracion = errorDuracion,
                campoConError = campo,
                intentoEnvio = it.intentoEnvio + 1,
                mensajeError = null
            )
        }
        return campo == null
    }

    private fun guardar(abrirVistaPrevia: Boolean) {
        val actual = _estado.value
        if (actual.guardando) return
        _estado.update { it.copy(confirmacion = null, guardando = true) }
        viewModelScope.launch {
            val usuario = usuarioRepository.usuarioActual().first()
            if (usuario == null) {
                _estado.update { it.copy(guardando = false, mensajeError = "Inicia sesión para guardar tu cápsula") }
                return@launch
            }
            val base = original
            val capsula = Capsula(
                id = actual.capsulaId ?: UUID.randomUUID().toString(),
                titulo = actual.titulo.trim(),
                descripcion = actual.descripcion.trim().ifEmpty { null },
                urlStream = actual.enlace.trim(),
                categoria = actual.categoria!!,
                duracionSegundos = actual.duracion.trim().toInt(),
                //CRE-02: la nueva parte en borrador; la que existía conserva su estado
                estado = actual.estadoCapsula,
                creadorId = base?.creadorId ?: usuario.id,
                creadaEn = base?.creadaEn ?: ahora(),
                publicadaEn = base?.publicadaEn
            )
            try {
                gestorCapsulas.guardar(capsula)
                original = capsula
                _estado.update {
                    it.copy(
                        capsulaId = capsula.id,
                        guardando = false,
                        terminada = !abrirVistaPrevia,
                        abrirVistaPrevia = if (abrirVistaPrevia) capsula.id else null
                    )
                }
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, mensajeError = "No se pudo guardar la cápsula") }
            }
        }
    }

    private fun eliminar() {
        val id = _estado.value.capsulaId ?: return
        _estado.update { it.copy(confirmacion = null, guardando = true) }
        viewModelScope.launch {
            when (val resultado = gestorCapsulas.eliminar(id)) {
                ResultadoCapsula.Listo -> _estado.update { it.copy(guardando = false, terminada = true) }
                is ResultadoCapsula.Error -> _estado.update { it.copy(guardando = false, mensajeError = resultado.mensaje) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                CapsulaEditarViewModel(
                    //sin capsulaId en la ruta es una cápsula nueva
                    capsulaId = createSavedStateHandle().get<String>(Rutas.ARG_CAPSULA_ID),
                    usuarioRepository = app.container.usuarioRepository,
                    capsulaRepository = app.container.capsulaRepository,
                    gestorCapsulas = app.container.gestorCapsulas
                )
            }
        }
    }
}
