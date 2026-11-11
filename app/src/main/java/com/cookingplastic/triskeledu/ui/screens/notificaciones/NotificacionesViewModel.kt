package com.cookingplastic.triskeledu.ui.screens.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.repository.NotificacionRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
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
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val diasSemana = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")

//P16: el historial del usuario conectado, de la más nueva a la más antigua (PER-03)
class NotificacionesViewModel(
    private val usuarioRepository: UsuarioRepository,
    private val notificacionRepository: NotificacionRepository,
    private val ahora: () -> Long = System::currentTimeMillis,
    private val zona: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _estado = MutableStateFlow(NotificacionesUiState())
    val estado: StateFlow<NotificacionesUiState> = _estado.asStateFlow()

    private var usuarioId: String? = null
    private var rol: RolUsuario? = null

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        val datos = usuarioRepository.usuarioActual().flatMapLatest { usuario ->
            usuarioId = usuario?.id
            rol = usuario?.rol
            if (usuario == null) return@flatMapLatest flowOf(Triple(emptyList<Notificacion>(), false, 0))
            val solicitudes = if (usuario.rol == RolUsuario.ADMINISTRADOR) usuarioRepository.solicitudesPendientes().map { it.size } else flowOf(0)
            combine(notificacionRepository.notificacionesDe(usuario.id), solicitudes) { lista, pendientes ->
                Triple(lista, usuario.rol == RolUsuario.ADMINISTRADOR, pendientes)
            }
        }
        viewModelScope.launch {
            datos.collect { (lista, esAdmin, pendientes) ->
                _estado.update {
                    it.copy(
                        filas = armarNotificaciones(lista, ahora(), zona),
                        sinLeer = lista.count { n -> !n.leida },
                        esAdministrador = esAdmin,
                        solicitudesPendientes = pendientes,
                        cargando = false
                    )
                }
            }
        }
    }

    fun onEvent(evento: NotificacionesUiEvent) {
        when (evento) {
            is NotificacionesUiEvent.OnTocar -> {
                val notificacion = evento.notificacion
                if (!notificacion.leida) viewModelScope.launch { notificacionRepository.marcarLeida(notificacion.id) }
                _estado.update { it.copy(destino = destinoDe(notificacion, rol)) }
            }
            NotificacionesUiEvent.OnMarcarTodasLeidas -> usuarioId?.let { id -> viewModelScope.launch { notificacionRepository.marcarTodasLeidas(id) } }
            NotificacionesUiEvent.OnDestinoAbierto -> _estado.update { it.copy(destino = null) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                NotificacionesViewModel(app.container.usuarioRepository, app.container.notificacionRepository)
            }
        }
    }
}

//Agrupa en Hoy / Esta semana / Antes y arma la fecha corta de cada una, de la más nueva a la más antigua
fun armarNotificaciones(lista: List<Notificacion>, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): List<FilaNotificacion> {
    val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
    return lista.sortedByDescending { it.creadaEn }.map { notificacion ->
        val momento = Instant.ofEpochMilli(notificacion.creadaEn).atZone(zona)
        val dias = ChronoUnit.DAYS.between(momento.toLocalDate(), hoy)
        val grupo = when {
            dias <= 0 -> GrupoNotificacion.HOY
            dias < 7 -> GrupoNotificacion.ESTA_SEMANA
            else -> GrupoNotificacion.ANTES
        }
        val fecha = when (grupo) {
            GrupoNotificacion.HOY -> "%02d:%02d".format(momento.hour, momento.minute)
            GrupoNotificacion.ESTA_SEMANA -> diasSemana[momento.dayOfWeek.value - 1]
            GrupoNotificacion.ANTES -> "${momento.dayOfMonth} ${meses[momento.monthValue - 1]}"
        }
        FilaNotificacion(notificacion, grupo, fecha)
    }
}

//PER-03: qué abre cada notificación. Las del lugar abren su detalle; las del admin, el punto o las solicitudes
fun destinoDe(notificacion: Notificacion, rol: RolUsuario?): DestinoNotificacion? {
    val puntoId = notificacion.puntoId
    return when (notificacion.tipo) {
        TipoNotificacion.PUNTO_LISTO -> puntoId?.let { DestinoNotificacion.EditarPunto(it) }
        TipoNotificacion.SOLICITUD -> if (rol == RolUsuario.ADMINISTRADOR) DestinoNotificacion.Solicitudes else null
        //un logro sin lugar es la subida de nivel: abre P24
        TipoNotificacion.LOGRO -> puntoId?.let { DestinoNotificacion.Detalle(it) } ?: DestinoNotificacion.Nivel
        TipoNotificacion.LUGAR_CERCANO, TipoNotificacion.NUEVA_CAPSULA -> puntoId?.let { DestinoNotificacion.Detalle(it) }
        //EXC-08 trae el lugar rechazado; el código de recuperación no trae nada
        TipoNotificacion.SISTEMA -> puntoId?.let { DestinoNotificacion.Detalle(it) }
    }
}
