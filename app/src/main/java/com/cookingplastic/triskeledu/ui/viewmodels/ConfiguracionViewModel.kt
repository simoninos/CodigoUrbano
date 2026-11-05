package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.repository.CapsulaRepository
import com.cookingplastic.triskeledu.data.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.event.ConfiguracionUiEvent
import com.cookingplastic.triskeledu.ui.state.ConfiguracionUiState
import com.cookingplastic.triskeledu.ui.validation.ReglaNivel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//Recibe tres repositorios: las preferencias, quién está conectado y su progreso (para el nivel y los temas)
class ConfiguracionViewModel(
    private val preferenciasRepository: PreferenciasRepository,
    private val usuarioRepository: UsuarioRepository,
    private val progresoRepository: ProgresoRepository,
    private val capsulaRepository: CapsulaRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(ConfiguracionUiState())
    val estado: StateFlow<ConfiguracionUiState> = _estado.asStateFlow()

    //el usuario conectado con sus puntos totales; sin sesión, nadie y 0 puntos
    @OptIn(ExperimentalCoroutinesApi::class)
    private val usuarioYPuntos: Flow<Pair<Usuario?, Int>> = usuarioRepository.usuarioActual().flatMapLatest { usuario ->
        if (usuario == null) flowOf(null to 0) else progresoRepository.totalPuntos(usuario.id).map { usuario to it }
    }

    //dos tareas en segundo plano, una por cada Flow que hay que escuchar
    init {
        viewModelScope.launch {
            //LVL-03 y RN-33: los temas desbloqueados salen del nivel actual o del más alto que alcanzó
            combine(preferenciasRepository.preferencias(), usuarioYPuntos) { preferencias, (usuario, puntos) ->
                val esAdmin = usuario?.rol == RolUsuario.ADMINISTRADOR
                val nivel = if (usuario == null || esAdmin) null else ReglaNivel.nivelDesde(puntos)
                val rol = usuario?.rol ?: RolUsuario.CLIENTE
                val temas = ReglaNivel.temasDesbloqueados(nivel?.numero ?: 1, preferencias.nivelMaximoAlcanzado, rol)
                Triple(preferencias, nivel, temas)
            }.collect { (preferencias, nivel, temas) ->
                _estado.update {
                    it.copy(
                        preferencias = preferencias,
                        tema = preferencias.tema,
                        temasDesbloqueados = temas,
                        nivel = nivel,
                        cargando = false
                    )
                }
            }
        }
        //CRE-01: cuántas cápsulas tiene el creador conectado en cada estado
        viewModelScope.launch {
            combine(usuarioRepository.usuarioActual(), capsulaRepository.capsulas()) { usuario, capsulas ->
                capsulas.filter { it.creadorId == usuario?.id }
            }.collect { propias ->
                _estado.update {
                    it.copy(
                        capsulasPublicadas = propias.count { c -> c.estado == EstadoCapsula.PUBLICADA },
                        capsulasBorrador = propias.count { c -> c.estado == EstadoCapsula.BORRADOR }
                    )
                }
            }
        }
        viewModelScope.launch {
            usuarioRepository.usuarioActual().collect { usuario ->
                _estado.update {
                    it.copy(
                        nombreUsuario = usuario?.username ?: "",
                        esAdministrador = usuario?.rol == RolUsuario.ADMINISTRADOR,
                        esCreador = usuario?.rol == RolUsuario.CREADOR,
                        //si no hay nadie conectado (por ejemplo, después de cerrar sesión), se vuelve al login
                        sesionCerrada = usuario == null
                    )
                }
            }
        }
    }

    fun onEvent(evento: ConfiguracionUiEvent) {
        //copy cambia solo el interruptor que se tocó; los otros 4 quedan iguales
        val actuales = _estado.value.preferencias
        when (evento) {
            //un tema bloqueado no responde (LVL-03)
            is ConfiguracionUiEvent.OnTemaChange ->
                if (evento.tema in _estado.value.temasDesbloqueados) guardar(actuales.copy(tema = evento.tema))
            is ConfiguracionUiEvent.OnTemaNocheChange -> guardar(actuales.copy(temaNoche = evento.valor))
            is ConfiguracionUiEvent.OnNotifLugaresCercanosChange -> guardar(actuales.copy(notifLugaresCercanos = evento.valor))
            is ConfiguracionUiEvent.OnNotifNuevasCapsulasChange -> guardar(actuales.copy(notifNuevasCapsulas = evento.valor))
            is ConfiguracionUiEvent.OnSeguirUbicacionChange -> guardar(actuales.copy(seguirUbicacion = evento.valor))
            is ConfiguracionUiEvent.OnMostrarBloqueadosChange -> guardar(actuales.copy(mostrarBloqueados = evento.valor))
            is ConfiguracionUiEvent.OnNotifSolicitudesChange -> guardar(actuales.copy(notifSolicitudes = evento.valor))
            is ConfiguracionUiEvent.OnNotifPuntosListosChange -> guardar(actuales.copy(notifPuntosListos = evento.valor))
            is ConfiguracionUiEvent.OnMostrarInactivosChange -> guardar(actuales.copy(mostrarInactivos = evento.valor))
            ConfiguracionUiEvent.OnCerrarSesion -> cerrarSesion()
        }
    }

    private fun guardar(nuevas: Preferencias) {
        //mientras carga no se guarda: todavía no se sabe de quién son las preferencias
        if (_estado.value.cargando) return

        viewModelScope.launch {
            _estado.update { it.copy(mensajeError = null) }
            try {
                //el estado no se toca aquí: el Flow del init trae lo guardado y actualiza la pantalla
                preferenciasRepository.guardar(nuevas)
            } catch (e: Exception) {
                _estado.update { it.copy(mensajeError = "No se pudo guardar el cambio") }
            }
        }
    }

    private fun cerrarSesion() {
        viewModelScope.launch {
            //usuarioActual avisa null y el init pone sesionCerrada = true
            usuarioRepository.cerrarSesion()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                ConfiguracionViewModel(
                    app.container.preferenciasRepository,
                    app.container.usuarioRepository,
                    RepositoriosProgreso.progreso(app),
                    app.container.capsulaRepository
                )
            }
        }
    }
}
