package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosCompartidos
import com.cookingplastic.triskeledu.data.location.UbicacionRepository
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.datosFaltantes
import com.cookingplastic.triskeledu.data.model.puedeActivarse
import com.cookingplastic.triskeledu.data.repository.CapsulaRepository
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.ui.event.AdminEditarUiEvent
import com.cookingplastic.triskeledu.ui.state.AdminEditarUiState
import com.cookingplastic.triskeledu.ui.validation.Validaciones
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminEditarViewModel(
    private val capsulaRepository: CapsulaRepository,
    private val puntoRepository: PuntoInteresRepository,
    private val ubicacionRepository: UbicacionRepository,
    //null = punto nuevo; con un id = editar ese punto
    puntoId: String? = null
) : ViewModel() {

    private val _estado = MutableStateFlow(AdminEditarUiState())
    val estado: StateFlow<AdminEditarUiState> = _estado.asStateFlow()

    //las cápsulas llenan el menú de "Video asociado"
    init {
        viewModelScope.launch {
            capsulaRepository.capsulas().collect { capsulas ->
                _estado.update { it.copy(capsulas = capsulas) }
            }
        }
        if (puntoId != null) cargarPunto(puntoId)
    }

    //first() lee el punto una sola vez: si siguiera escuchando, cualquier cambio en la lista
    //borraría lo que el admin está escribiendo
    private fun cargarPunto(id: String) {
        viewModelScope.launch {
            val punto = puntoRepository.obtenerPorId(id).first()
            if (punto == null) {
                _estado.update { it.copy(mensajeError = "No se encontró el punto") }
                return@launch
            }
            _estado.update {
                it.copy(
                    id = punto.id,
                    esEdicion = true,
                    nombre = punto.nombre,
                    tipo = punto.tipo,
                    descripcion = punto.descripcion ?: "",
                    latitud = punto.ubicacion.latitud.toString(),
                    longitud = punto.ubicacion.longitud.toString(),
                    altitud = punto.ubicacion.altitud.toString(),
                    descubreMas = punto.descubreMasUrl ?: "",
                    imagenUri = punto.imagenUri,
                    capsulaId = punto.capsulaId,
                    codigoQr = punto.codigoQr,
                    activo = punto.activo
                )
            }
        }
    }

    fun onEvent(evento: AdminEditarUiEvent) {
        when (evento) {
            is AdminEditarUiEvent.OnNombreChange -> _estado.update {
                it.copy(nombre = evento.valor, errorNombre = Validaciones.validarNombrePunto(evento.valor))
            }
            is AdminEditarUiEvent.OnTipoChange -> _estado.update {
                it.copy(tipo = evento.valor, errorTipo = Validaciones.validarTipo(evento.valor))
            }
            is AdminEditarUiEvent.OnDescripcionChange -> _estado.update {
                it.copy(descripcion = evento.valor, errorDescripcion = Validaciones.validarDescripcion(evento.valor))
            }
            //en Chile se escribe con coma; se cambia por punto para que toDoubleOrNull lo entienda
            is AdminEditarUiEvent.OnLatitudChange -> {
                val texto = evento.valor.replace(',', '.')
                _estado.update { it.copy(latitud = texto, errorLatitud = Validaciones.validarLatitud(texto)) }
            }
            is AdminEditarUiEvent.OnLongitudChange -> {
                val texto = evento.valor.replace(',', '.')
                _estado.update { it.copy(longitud = texto, errorLongitud = Validaciones.validarLongitud(texto)) }
            }
            is AdminEditarUiEvent.OnAltitudChange -> {
                val texto = evento.valor.replace(',', '.')
                _estado.update { it.copy(altitud = texto, errorAltitud = Validaciones.validarAltitud(texto)) }
            }
            is AdminEditarUiEvent.OnDescubreMasChange -> _estado.update {
                it.copy(descubreMas = evento.valor, errorDescubreMas = Validaciones.validarDescubreMas(evento.valor.trim()))
            }
            is AdminEditarUiEvent.OnImagenElegida -> _estado.update { it.copy(imagenUri = evento.uri, avisoImagen = null) }
            AdminEditarUiEvent.OnQuitarImagen -> _estado.update { it.copy(imagenUri = null) }
            is AdminEditarUiEvent.OnCapsulaChange -> _estado.update { it.copy(capsulaId = evento.capsulaId) }
            is AdminEditarUiEvent.OnActivoChange -> _estado.update { it.copy(activo = evento.valor, mensajeError = null) }
            //el QR lleva el id del punto: al escanearlo, la app sabe qué punto es
            AdminEditarUiEvent.OnGenerarQr -> _estado.update { it.copy(codigoQr = it.id) }
            AdminEditarUiEvent.OnUsarMiUbicacion -> usarMiUbicacion()
            AdminEditarUiEvent.OnPermisoCamaraDenegado -> _estado.update {
                it.copy(avisoImagen = "Sin permiso de cámara no se puede tomar la foto")
            }
            AdminEditarUiEvent.OnPermisoUbicacionDenegado -> _estado.update {
                it.copy(avisoUbicacion = "Sin permiso de ubicación no se puede usar tu posición")
            }
            AdminEditarUiEvent.OnGuardar -> guardar()
        }
    }

    private fun usarMiUbicacion() {
        viewModelScope.launch {
            _estado.update { it.copy(buscandoUbicacion = true, avisoUbicacion = null) }
            val ubicacion = try {
                ubicacionRepository.ultimaUbicacion()
            } catch (e: Exception) {
                null
            }
            if (ubicacion == null) {
                _estado.update {
                    it.copy(buscandoUbicacion = false, avisoUbicacion = "No se pudo obtener tu ubicación. Revisa que el GPS esté encendido")
                }
                return@launch
            }
            //Locale.ROOT para que el número salga con punto y no con coma, aunque el teléfono esté en español
            val latitud = String.format(Locale.ROOT, "%.6f", ubicacion.latitud)
            val longitud = String.format(Locale.ROOT, "%.6f", ubicacion.longitud)
            val altitud = String.format(Locale.ROOT, "%.1f", ubicacion.altitud)
            _estado.update {
                it.copy(
                    buscandoUbicacion = false,
                    latitud = latitud,
                    longitud = longitud,
                    altitud = altitud,
                    errorLatitud = Validaciones.validarLatitud(latitud),
                    errorLongitud = Validaciones.validarLongitud(longitud),
                    errorAltitud = Validaciones.validarAltitud(altitud)
                )
            }
        }
    }

    private fun guardar() {
        val actual = _estado.value
        if (!actual.puedeGuardar) return
        //puedeGuardar ya revisó que hay tipo; esto es para que Kotlin lo sepa
        val tipo = actual.tipo ?: return

        //toDouble no falla porque las coordenadas ya pasaron por Validaciones
        //los textos opcionales vacíos van como null, igual que en PuntoInteres
        val punto = PuntoInteres(
            id = actual.id,
            nombre = actual.nombre.trim(),
            descripcion = actual.descripcion.trim().ifBlank { null },
            tipo = tipo,
            ubicacion = Ubicacion(
                latitud = actual.latitud.toDouble(),
                longitud = actual.longitud.toDouble(),
                altitud = actual.altitud.toDouble()
            ),
            imagenUri = actual.imagenUri,
            descubreMasUrl = actual.descubreMas.trim().ifBlank { null },
            codigoQr = actual.codigoQr,
            capsulaId = actual.capsulaId,
            activo = actual.activo
        )

        //RN-14 (regla de Simon): un punto activo tiene que tener imagen, video y QR
        if (punto.activo && !punto.puedeActivarse()) {
            _estado.update { it.copy(mensajeError = "Para activarlo falta: ${unirConY(punto.datosFaltantes())}") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, mensajeError = null) }
            try {
                //mismo id = se actualiza el punto; id nuevo = se crea
                puntoRepository.guardar(punto)
                _estado.update { it.copy(guardando = false, guardado = true) }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(guardando = false, mensajeError = "No se pudo guardar el punto, intenta de nuevo")
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                AdminEditarViewModel(
                    capsulaRepository = app.container.capsulaRepository,
                    //la misma instancia que usan la lista del admin y el mapa: lo que se guarda aquí se ve allá
                    puntoRepository = app.container.puntoInteresRepository,
                    ubicacionRepository = RepositoriosCompartidos.ubicacion(app),
                    //la ruta admin/editar?puntoId=... deja el id en el SavedStateHandle; sin él, es un punto nuevo
                    puntoId = createSavedStateHandle().get<String>(Rutas.ARG_PUNTO_ID)
                )
            }
        }
    }
}
