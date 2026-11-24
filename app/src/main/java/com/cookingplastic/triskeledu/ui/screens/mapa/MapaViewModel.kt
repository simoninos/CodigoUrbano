package com.cookingplastic.triskeledu.ui.screens.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosCompartidos
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.location.UbicacionRepository
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.model.puntosTotales
import com.cookingplastic.triskeledu.data.notificaciones.GeneradorNotificaciones
import com.cookingplastic.triskeledu.data.notificaciones.ReglaAvisos
import com.cookingplastic.triskeledu.domain.repository.CapsulaRepository
import com.cookingplastic.triskeledu.data.repository.NotificacionRepository
import com.cookingplastic.triskeledu.domain.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.domain.repository.ProgresoRepository
import com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import com.cookingplastic.triskeledu.navigation.PedidoMapa
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.domain.rules.ReglaMapa
import com.cookingplastic.triskeledu.domain.rules.ReglaNivel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Acercamiento con que se abre el mapa en un lugar pedido desde Mis descubrimientos
private const val ZOOM_LUGAR = 17.0

// ViewModel de la pantalla 3 (Mapa).
// Junta cuatro fuentes en un solo estado: puntos activos, ubicación, usuario conectado con su
// progreso, y preferencias.
@OptIn(ExperimentalCoroutinesApi::class)
class MapaViewModel(
    private val puntoInteresRepository: PuntoInteresRepository,
    private val ubicacionRepository: UbicacionRepository,
    private val usuarioRepository: UsuarioRepository,
    private val preferenciasRepository: PreferenciasRepository,
    private val progresoRepository: ProgresoRepository,
    // LUG-03: pedido de Mis descubrimientos para abrir el mapa en un lugar
    private val pedidoMapa: PedidoMapa = PedidoMapa(),
    // CRE-07: las cápsulas dicen qué puntos inactivos están Sin cápsula
    private val capsulaRepository: CapsulaRepository,
    // PER-03: el contador de no leídas de la campana
    private val notificacionRepository: NotificacionRepository? = null,
    // RN-24: "Lugar cercano" con la app abierta
    private val generadorNotificaciones: GeneradorNotificaciones? = null
) : ViewModel() {

    // Privado y mutable: solo el ViewModel escribe. Público y de solo lectura: la pantalla observa.
    private val _estado = MutableStateFlow(MapaUiState())
    val estado: StateFlow<MapaUiState> = _estado.asStateFlow()

    // U-04: la ubicación se escucha solo con permiso. Cuando el permiso cambia,
    // flatMapLatest deja el Flow anterior y empieza uno nuevo.
    private val permisoConcedido = MutableStateFlow(false)

    private val ubicacion: Flow<Ubicacion?> = permisoConcedido.flatMapLatest { concedido ->
        if (!concedido) {
            flowOf(null)
        } else {
            ubicacionRepository.ubicaciones()
                .map<Ubicacion, Ubicacion?> { it }
                .onStart { emit(ubicacionRepository.ultimaUbicacion()) }
                .catch {
                    // U-06 y M-18: sin ubicación, el mapa sigue funcionando y avisa con un mensaje
                    _estado.update { it.copy(mensajeError = "No se pudo obtener tu ubicación. Revisa que el GPS esté activo") }
                    emit(null)
                }
        }
    }

    // El progreso depende del usuario conectado: al cambiar de usuario, flatMapLatest deja de
    // escuchar el progreso anterior y empieza con el del nuevo. Sin sesión no hay progreso.
    private val usuarioConProgreso: Flow<Pair<Usuario?, List<ProgresoPunto>>> =
        usuarioRepository.usuarioActual().flatMapLatest { usuario ->
            if (usuario == null) {
                flowOf(null to emptyList())
            } else {
                progresoRepository.progresoDe(usuario.id).map { filas -> usuario to filas }
            }
        }

    // Últimas preferencias recibidas; el botón de seguimiento guarda una copia con el valor cambiado
    private var preferenciasActuales: Preferencias? = null

    init {
        // LUG-03: el lugar pedido queda elegido (con su tarjeta) y la cámara parte ahí al abrir el mapa
        viewModelScope.launch {
            pedidoMapa.puntoId.filterNotNull().collect { puntoId ->
                val punto = puntoInteresRepository.obtenerPorId(puntoId).first()
                if (punto != null) {
                    _estado.update {
                        it.copy(
                            seleccionadoId = punto.id,
                            camara = CamaraMapa(punto.ubicacion.latitud, punto.ubicacion.longitud, it.camara?.zoom ?: ZOOM_LUGAR)
                        )
                    }
                }
                pedidoMapa.atendido()
            }
        }
        viewModelScope.launch {
            // combine emite un estado nuevo cada vez que cualquiera de las cinco fuentes cambia
            combine(
                puntoInteresRepository.obtenerTodos(),
                capsulaRepository.capsulas(),
                ubicacion,
                usuarioConProgreso,
                preferenciasRepository.preferencias()
            ) { puntos, capsulas, ubicacionActual, (usuario, progreso), preferencias ->
                preferenciasActuales = preferencias
                // Para buscar rápido la fila de cada punto: puntoId -> fila
                val progresoPorPunto = progreso.associateBy { it.puntoId }
                val capsulaPorId = capsulas.associateBy { it.id }
                val puntosMapa = puntos.mapNotNull { punto ->
                    // RN-27: un punto activo siempre tiene su cápsula publicada (RN-14); los demás se calculan
                    val estadoAdmin = if (punto.activo) EstadoAdminPunto.ACTIVO
                    else EstadoAdminPunto.desde(punto, punto.capsulaId?.let { capsulaPorId[it] })
                    // RN-15 y CRE-07: el cliente solo ve los Activos; el creador y el admin, también los Sin cápsula
                    // ADM-11: el administrador también ve los Inactivos, salvo que los oculte en Configuración
                    if (!ReglaMapa.puntoVisible(estadoAdmin, usuario?.rol, preferencias.mostrarInactivos)) return@mapNotNull null
                    PuntoMapa(
                        punto = punto,
                        // M-04: el estado sale del progreso del usuario en ese punto
                        estado = EstadoPunto.desde(progresoPorPunto[punto.id]),
                        distanciaMetros = ubicacionActual?.let {
                            ubicacionRepository.distanciaMetros(it, punto.ubicacion)
                        },
                        estadoAdmin = estadoAdmin
                    )
                }
                // RN-24: avisa los puntos activos sin escanear a 200 m o menos (el generador cuida una vez al día)
                if (usuario != null && usuario.rol != RolUsuario.ADMINISTRADOR) revisarCercania(usuario.id, puntosMapa)
                _estado.update {
                    it.copy(
                        puntos = puntosMapa,
                        ubicacionUsuario = ubicacionActual ?: it.ubicacionUsuario,
                        nombreUsuario = usuario?.username ?: "",
                        rol = usuario?.rol,
                        // M-13: suma todo lo ganado, igual que Mis lugares (también en puntos ya desactivados, RN-15)
                        totalPuntos = progreso.sumOf { it.puntosTotales() },
                        // LVL-04 y LVL-05: el nivel sale de los puntos; el administrador no tiene
                        nivel = if (usuario == null || usuario.rol == RolUsuario.ADMINISTRADOR) null
                        else ReglaNivel.nivelDesde(progreso.sumOf { it.puntosTotales() }),
                        seguirUbicacion = preferencias.seguirUbicacion,
                        mostrarBloqueados = preferencias.mostrarBloqueados,
                        cargando = false
                    )
                }
            }
                .catch {
                    _estado.update { it.copy(cargando = false, mensajeError = "No se pudieron cargar los puntos") }
                }
                .collect { }
        }
        // PER-03: el contador de la campana
        viewModelScope.launch {
            usuarioRepository.usuarioActual()
                .flatMapLatest { usuario ->
                    val repositorio = notificacionRepository
                    if (usuario == null || repositorio == null) flowOf(0) else repositorio.sinLeer(usuario.id)
                }
                .catch { emit(0) }
                .collect { cantidad -> _estado.update { it.copy(notificacionesSinLeer = cantidad) } }
        }
        // ADM-10: la campana del administrador cuenta las solicitudes de creador pendientes
        viewModelScope.launch {
            usuarioRepository.usuarioActual()
                .flatMapLatest { usuario ->
                    if (usuario?.rol == RolUsuario.ADMINISTRADOR) usuarioRepository.solicitudesPendientes().map { it.size }
                    else flowOf(0)
                }
                .catch { emit(0) }
                .collect { cantidad -> _estado.update { it.copy(solicitudesPendientes = cantidad) } }
        }
    }

    // puntos ya revisados hoy en esta sesión: así no se consulta el historial en cada lectura del GPS
    private val revisadosHoy = mutableSetOf<String>()

    private fun revisarCercania(usuarioId: String, puntos: List<PuntoMapa>) {
        val generador = generadorNotificaciones ?: return
        val dia = java.time.LocalDate.now().toString()
        puntos.filter { it.activo && it.estado == EstadoPunto.BLOQUEADO }
            .filter { (it.distanciaMetros ?: Double.MAX_VALUE) <= ReglaAvisos.RADIO_CERCANO_METROS }
            .forEach { punto ->
                val clave = "$dia-$usuarioId-${punto.punto.id}"
                if (revisadosHoy.add(clave)) {
                    viewModelScope.launch {
                        try {
                            generador.lugarCercano(usuarioId, punto.punto, punto.distanciaMetros ?: return@launch)
                        } catch (e: Exception) {
                            revisadosHoy.remove(clave)
                        }
                    }
                }
            }
    }

    fun onEvent(evento: MapaUiEvent) {
        when (evento) {
            is MapaUiEvent.OnClickPunto -> _estado.update { it.copy(seleccionadoId = evento.puntoId) }
            MapaUiEvent.OnCerrarTarjeta -> _estado.update { it.copy(seleccionadoId = null) }
            MapaUiEvent.OnCentrar -> {
                if (_estado.value.ubicacionUsuario == null) {
                    _estado.update { it.copy(mensajeError = "Todavía no tenemos tu ubicación") }
                } else {
                    _estado.update { it.copy(solicitudCentrar = it.solicitudCentrar + 1) }
                }
            }
            MapaUiEvent.OnAlternarSeguimiento -> alternarSeguimiento()
            is MapaUiEvent.OnCamaraGuardada -> _estado.update { it.copy(camara = evento.camara) }
            MapaUiEvent.OnErrorMostrado -> _estado.update { it.copy(mensajeError = null) }
            // U-05 y EXC-01: sin permiso, el aviso "Activa la ubicación para encontrar lugares" del mapa lo explica
            is MapaUiEvent.OnPermisoUbicacion -> permisoConcedido.value = evento.concedido
        }
    }

    // M-20 y M-21: cambia la preferencia seguirUbicacion, la misma del interruptor de Configuración
    private fun alternarSeguimiento() {
        val actuales = preferenciasActuales ?: return
        val seguir = !actuales.seguirUbicacion
        viewModelScope.launch {
            try {
                // El Flow de preferencias trae el cambio de vuelta y actualiza el estado
                preferenciasRepository.guardar(actuales.copy(seguirUbicacion = seguir))
                _estado.update {
                    it.copy(
                        // M-22: al activarlo, el mapa vuelve al usuario de inmediato
                        solicitudCentrar = if (seguir && it.ubicacionUsuario != null) it.solicitudCentrar + 1 else it.solicitudCentrar,
                        mensajeError = if (seguir) "Seguimiento activado" else "Seguimiento desactivado"
                    )
                }
            } catch (e: Exception) {
                _estado.update { it.copy(mensajeError = "No se pudo cambiar el seguimiento") }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                MapaViewModel(
                    puntoInteresRepository = app.container.puntoInteresRepository,
                    ubicacionRepository = RepositoriosCompartidos.ubicacion(app),
                    usuarioRepository = app.container.usuarioRepository,
                    preferenciasRepository = app.container.preferenciasRepository,
                    progresoRepository = RepositoriosProgreso.progreso(app),
                    pedidoMapa = app.container.pedidoMapa,
                    capsulaRepository = app.container.capsulaRepository,
                    notificacionRepository = app.container.notificacionRepository,
                    generadorNotificaciones = app.container.generadorNotificaciones
                )
            }
        }
    }
}
