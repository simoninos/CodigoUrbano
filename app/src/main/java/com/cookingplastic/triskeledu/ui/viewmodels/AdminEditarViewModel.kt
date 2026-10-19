package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.repository.CapsulaRepository
import com.cookingplastic.triskeledu.ui.event.AdminEditarUiEvent
import com.cookingplastic.triskeledu.ui.state.AdminEditarUiState
import com.cookingplastic.triskeledu.ui.validation.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminEditarViewModel(
    private val capsulaRepository: CapsulaRepository,
    //"guardar" llega como parámetro, igual que alCambiar en el Interruptor. Así no dependemos de un repositorio que todavía no existe
    private val guardarPunto: suspend (PuntoInteres) -> Unit
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
            is AdminEditarUiEvent.OnImagenElegida -> _estado.update { it.copy(imagenUri = evento.uri) }
            AdminEditarUiEvent.OnQuitarImagen -> _estado.update { it.copy(imagenUri = null) }
            is AdminEditarUiEvent.OnCapsulaChange -> _estado.update { it.copy(capsulaId = evento.capsulaId) }
            is AdminEditarUiEvent.OnActivoChange -> _estado.update { it.copy(activo = evento.valor) }
            //el QR lleva el id del punto: al escanearlo, la app sabe qué punto es
            AdminEditarUiEvent.OnGenerarQr -> _estado.update { it.copy(codigoQr = it.id) }
            AdminEditarUiEvent.OnGuardar -> guardar()
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

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, mensajeError = null) }
            try {
                guardarPunto(punto)
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
                    //TEMPORAL: falta PuntoInteresRepository de Simon. Cuando exista, aquí va su función guardar
                    guardarPunto = {}
                )
            }
        }
    }
}
