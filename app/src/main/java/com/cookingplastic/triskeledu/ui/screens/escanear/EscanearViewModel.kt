package com.cookingplastic.triskeledu.ui.screens.escanear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosCompartidos
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.location.UbicacionRepository
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.domain.repository.ProgresoRepository
import com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import com.cookingplastic.triskeledu.domain.rules.ReglaEscaneo
import com.cookingplastic.triskeledu.domain.rules.ReglaQr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.combine

//Resultado de revisar un QR
sealed interface ResultadoEscaneo {
    data class Desbloqueado(
        val punto: PuntoInteres,
        val distanciaMetros: Double
    ) : ResultadoEscaneo

    //EXC-04: ya lo había escaneado; no se mide la distancia ni se suma
    data class YaDescubierto(val punto: PuntoInteres) : ResultadoEscaneo

    data class Rechazado(val mensaje: String) : ResultadoEscaneo
}

//La validación del escaneo: buscar el lugar por su QR, medir la distancia, aplicar la regla
//de 100 m y registrar el progreso. Está aparte para poder probarla con las Fakes, sin Android
class ProcesadorEscaneo(
    private val puntoInteresRepository: PuntoInteresRepository,
    private val ubicacionRepository: UbicacionRepository,
    private val progresoRepository: ProgresoRepository
) {
    suspend fun procesar(codigo: String, usuarioId: String?): ResultadoEscaneo {
        if (usuarioId == null) return ResultadoEscaneo.Rechazado("Inicia sesión para guardar tus lugares")

        //EXC-04: un QR que no es del juego, o de un lugar desactivado o eliminado, no desbloquea nada
        val punto = puntoInteresRepository.obtenerPorCodigoQr(codigo)
            ?: return ResultadoEscaneo.Rechazado(if (ReglaQr.esCodigoDelJuego(codigo)) LUGAR_NO_DISPONIBLE else CODIGO_AJENO)
        if (!punto.activo) return ResultadoEscaneo.Rechazado(LUGAR_NO_DISPONIBLE)

        //EXC-04: si ya lo tenía, se abre su detalle aunque esté lejos, y no suma de nuevo
        if (progresoRepository.progresoDePunto(usuarioId, punto.id).first() != null) {
            return ResultadoEscaneo.YaDescubierto(punto)
        }

        //sin GPS no se escanea
        val ubicacion = ubicacionRepository.ultimaUbicacion()
            ?: return ResultadoEscaneo.Rechazado("Activa el GPS para escanear")
        val distancia = ubicacionRepository.distanciaMetros(ubicacion, punto.ubicacion)

        //regla de los 100 m
        val rechazo = ReglaEscaneo.mensajeRechazo(distancia)
        if (rechazo != null) return ResultadoEscaneo.Rechazado(rechazo)

        //fecha, ubicación, +10 puntos y sincronizado = false; si ya existía, no suma de nuevo
        progresoRepository.registrarEscaneo(usuarioId, punto.id, ubicacion)
        return ResultadoEscaneo.Desbloqueado(punto, distancia)
    }

    companion object {
        //EXC-04: los mensajes del QR que no corresponde
        const val CODIGO_AJENO = "Este código no es de Aprendo IA"
        const val LUGAR_NO_DISPONIBLE = "Este lugar no está disponible"
        const val YA_DESCUBIERTO = "Ya descubriste este lugar"
    }
}

//Guarda el estado de Escanear. Como es un ViewModel, no se pierde al girar el teléfono
@OptIn(ExperimentalCoroutinesApi::class)
class EscanearViewModel(
    private val procesador: ProcesadorEscaneo,
    private val ubicacionRepository: UbicacionRepository,
    usuarioRepository: UsuarioRepository
) : ViewModel() {

    //privado y mutable: solo el ViewModel lo cambia
    private val _estado = MutableStateFlow(EscanearUiState())
    //público y de solo lectura: la pantalla lo observa
    val estado: StateFlow<EscanearUiState> = _estado.asStateFlow()

    private var usuarioId: String? = null
    private val permisoUbicacion = MutableStateFlow(false)
    private val gpsEncendido = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            usuarioRepository.usuarioActual().collect { usuario -> usuarioId = usuario?.id }
        }
        //el estado del GPS se muestra arriba; escucharlo además deja lista la última ubicación
        viewModelScope.launch {
            combine(permisoUbicacion, gpsEncendido) { concedido, encendido -> concedido to encendido }.flatMapLatest { (concedido, encendido) ->
                if (!concedido) {
                    flowOf(EstadoGps.SIN_PERMISO)
                } else if (!encendido) {
                    flowOf(EstadoGps.APAGADO)
                } else {
                    ubicacionRepository.ubicaciones()
                        .map { EstadoGps.ACTIVO }
                        .onStart { emit(EstadoGps.BUSCANDO) }
                        .catch { emit(EstadoGps.SIN_SENAL) }
                }
            }.collect { gps -> _estado.update { it.copy(gps = gps) } }
        }
    }

    fun onEvent(evento: EscanearUiEvent) {
        when (evento) {
            is EscanearUiEvent.OnPermisoCamara -> _estado.update { it.copy(permisoCamara = evento.concedido) }
            is EscanearUiEvent.OnPermisoUbicacion -> permisoUbicacion.value = evento.concedido
            is EscanearUiEvent.OnGpsEncendido -> gpsEncendido.value = evento.encendido
            is EscanearUiEvent.OnCodigoLeido -> codigoLeido(evento.codigo)
            EscanearUiEvent.OnEscanearOtro -> _estado.update {
                it.copy(codigoLeido = null, mensajeRechazo = null, encontrado = null, yaDescubierto = null)
            }
        }
    }

    private fun codigoLeido(codigo: String) {
        //la cámara ve el mismo QR muchas veces por segundo: solo se toma el primero
        val actual = _estado.value
        if (actual.procesando || actual.codigoLeido != null) return
        //EXC-01 y RN-05: sin ubicación la cámara no escanea; el aviso de abajo explica cómo activarla
        if (actual.sinUbicacion) return
        val limpio = codigo.trim()
        if (limpio.isEmpty()) return
        _estado.update { it.copy(codigoLeido = limpio, procesando = true, mensajeRechazo = null) }

        viewModelScope.launch {
            val resultado = try {
                procesador.procesar(limpio, usuarioId)
            } catch (e: Exception) {
                ResultadoEscaneo.Rechazado("No se pudo registrar el escaneo, intenta de nuevo")
            }
            _estado.update {
                when (resultado) {
                    is ResultadoEscaneo.Desbloqueado -> it.copy(
                        procesando = false,
                        encontrado = LugarEncontrado(
                            puntoId = resultado.punto.id,
                            nombre = resultado.punto.nombre,
                            tipo = resultado.punto.tipo.descripcion,
                            distanciaMetros = resultado.distanciaMetros.roundToInt()
                        )
                    )
                    is ResultadoEscaneo.YaDescubierto -> it.copy(
                        procesando = false,
                        yaDescubierto = LugarYaDescubierto(resultado.punto.id, resultado.punto.nombre)
                    )
                    is ResultadoEscaneo.Rechazado -> it.copy(procesando = false, mensajeRechazo = resultado.mensaje)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                val ubicacion = RepositoriosCompartidos.ubicacion(app)
                EscanearViewModel(
                    procesador = ProcesadorEscaneo(
                        puntoInteresRepository = app.container.puntoInteresRepository,
                        ubicacionRepository = ubicacion,
                        progresoRepository = RepositoriosProgreso.progreso(app)
                    ),
                    ubicacionRepository = ubicacion,
                    usuarioRepository = app.container.usuarioRepository
                )
            }
        }
    }
}