package com.cookingplastic.triskeledu.ui.screens.admin

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
import com.cookingplastic.triskeledu.domain.repository.CapsulaRepository
import com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.navigation.Rutas
import com.cookingplastic.triskeledu.domain.rules.ReglaCapsula
import com.cookingplastic.triskeledu.domain.rules.ReglaQr
import com.cookingplastic.triskeledu.domain.rules.Validaciones
import com.cookingplastic.triskeledu.domain.rules.primerCampoConError
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    //ADM-07: el menú de cápsulas solo ofrece las PUBLICADAS y libres (de cualquier creador)
    init {
        viewModelScope.launch {
            combine(capsulaRepository.capsulas(), puntoRepository.obtenerTodos()) { capsulas, puntos -> capsulas to puntos }
                .collect { (capsulas, puntos) ->
                    //al editar, la cápsula que ya tiene este punto también aparece
                    _estado.update { it.copy(capsulas = ReglaCapsula.capsulasParaPunto(capsulas, puntos, puntoId ?: it.id)) }
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
            is AdminEditarUiEvent.OnImagenElegida -> _estado.update { it.copy(imagenUri = evento.uri, avisoImagen = null, errorImagen = null) }
            //sin cápsula el punto queda Sin cápsula: no puede quedar activo (RN-14, RN-22)
            is AdminEditarUiEvent.OnCapsulaChange -> _estado.update {
                it.copy(capsulaId = evento.capsulaId, activo = if (evento.capsulaId == null) false else it.activo)
            }
            is AdminEditarUiEvent.OnActivoChange -> _estado.update {
                if (evento.valor && !it.puedeActivar) it else it.copy(activo = evento.valor, mensajeError = null)
            }
            AdminEditarUiEvent.OnUsarMiUbicacion -> usarMiUbicacion()
            AdminEditarUiEvent.OnAbrirSelectorUbicacion -> abrirSelector()
            AdminEditarUiEvent.OnCerrarSelectorUbicacion -> _estado.update { it.copy(selectorUbicacion = null) }
            is AdminEditarUiEvent.OnUbicacionElegida -> elegirUbicacion(evento.latitud, evento.longitud)
            is AdminEditarUiEvent.OnQrDescargado -> _estado.update {
                it.copy(avisoQr = evento.dondeQuedo?.let { lugar -> "QR guardado en $lugar" } ?: "No se pudo guardar el QR")
            }
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

    //P19 parte en la ubicación escrita; si no hay, en la del administrador; si tampoco, en La Moneda
    private fun abrirSelector() {
        val actual = _estado.value
        val escrita = coordenadasValidas(actual)
        if (escrita != null) {
            _estado.update { it.copy(selectorUbicacion = escrita) }
            return
        }
        viewModelScope.launch {
            val mia = try {
                ubicacionRepository.ultimaUbicacion()
            } catch (e: Exception) {
                null
            }
            _estado.update { it.copy(selectorUbicacion = mia ?: CENTRO_POR_DEFECTO) }
        }
    }

    //ADM-03: llena latitud y longitud; la altitud se conserva, o queda en 0 si no había (propuesta del ERS 7)
    private fun elegirUbicacion(latitud: Double, longitud: Double) {
        val textoLatitud = String.format(Locale.ROOT, "%.6f", latitud)
        val textoLongitud = String.format(Locale.ROOT, "%.6f", longitud)
        _estado.update {
            val altitud = if (Validaciones.validarAltitud(it.altitud) == null) it.altitud else "0"
            it.copy(
                selectorUbicacion = null,
                latitud = textoLatitud,
                longitud = textoLongitud,
                altitud = altitud,
                errorUbicacion = null,
                errorLatitud = Validaciones.validarLatitud(textoLatitud),
                errorLongitud = Validaciones.validarLongitud(textoLongitud),
                errorAltitud = Validaciones.validarAltitud(altitud)
            )
        }
    }

    private fun coordenadasValidas(actual: AdminEditarUiState): Ubicacion? {
        val latitud = actual.latitud.toDoubleOrNull() ?: return null
        val longitud = actual.longitud.toDoubleOrNull() ?: return null
        if (Validaciones.validarLatitud(actual.latitud) != null || Validaciones.validarLongitud(actual.longitud) != null) return null
        return Ubicacion(latitud, longitud, actual.altitud.toDoubleOrNull() ?: 0.0)
    }

    //RN-18: calcula el error de cada campo, también de los vacíos que no se han tocado
    private fun revisarTodo(actual: AdminEditarUiState): AdminEditarUiState {
        val errorNombre = Validaciones.validarNombrePunto(actual.nombre)
        val errorTipo = Validaciones.validarTipo(actual.tipo)
        val errorDescripcion = Validaciones.validarDescripcion(actual.descripcion)
        val errorImagen = if (actual.imagenUri == null) "Agrega una imagen del punto" else null
        //ADM-03: sin latitud ni longitud no se marca cada campo, se pide elegir la ubicación
        val sinUbicacion = actual.latitud.isBlank() && actual.longitud.isBlank()
        val errorUbicacion = if (sinUbicacion) "Elige la ubicación del punto" else null
        val errorLatitud = if (sinUbicacion) null else Validaciones.validarLatitud(actual.latitud)
        val errorLongitud = if (sinUbicacion) null else Validaciones.validarLongitud(actual.longitud)
        val errorAltitud = Validaciones.validarAltitud(actual.altitud)
        val errorDescubreMas = Validaciones.validarDescubreMas(actual.descubreMas.trim())
        return actual.copy(
            errorNombre = errorNombre,
            errorTipo = errorTipo,
            errorDescripcion = errorDescripcion,
            errorImagen = errorImagen,
            errorUbicacion = errorUbicacion,
            errorLatitud = errorLatitud,
            errorLongitud = errorLongitud,
            errorAltitud = errorAltitud,
            errorDescubreMas = errorDescubreMas,
            campoConError = primerCampoConError(
                CampoPunto.NOMBRE to errorNombre,
                CampoPunto.TIPO to errorTipo,
                CampoPunto.DESCRIPCION to errorDescripcion,
                CampoPunto.IMAGEN to errorImagen,
                CampoPunto.LATITUD to (errorUbicacion ?: errorLatitud),
                CampoPunto.LONGITUD to errorLongitud,
                CampoPunto.ALTITUD to errorAltitud,
                CampoPunto.DESCUBRE_MAS to errorDescubreMas
            )
        )
    }

    private fun guardar() {
        if (_estado.value.guardando) return
        val actual = revisarTodo(_estado.value)
        //con errores: se muestran todos, el foco va al primero y no se guarda
        if (actual.campoConError != null) {
            _estado.update { actual.copy(intentoEnvio = it.intentoEnvio + 1) }
            return
        }
        //ya se revisó que hay tipo e imagen; esto es para que Kotlin lo sepa
        val tipo = actual.tipo ?: return
        val imagen = actual.imagenUri ?: return

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
            imagenUri = imagen,
            descubreMasUrl = actual.descubreMas.trim().ifBlank { null },
            //ADM-08: un punto nuevo recibe su QR al guardarse (se completa abajo); al editar se conserva
            codigoQr = actual.codigoQr ?: "",
            capsulaId = actual.capsulaId,
            activo = actual.activo
        )

        //RN-14: un punto activo tiene que tener imagen, QR y una cápsula publicada
        val capsula = actual.capsulas.find { it.id == punto.capsulaId }
        if (punto.activo && !punto.puedeActivarse(capsula)) {
            _estado.update { it.copy(mensajeError = "Para activarlo falta: ${unirConY(punto.datosFaltantes(capsula))}") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, mensajeError = null) }
            try {
                //el código se elige recién ahora, con los puntos actuales, para no repetir uno
                val codigo = actual.codigoQr ?: ReglaQr.siguienteCodigo(puntoRepository.obtenerTodos().first().map { it.codigoQr })
                //mismo id = se actualiza el punto; id nuevo = se crea
                puntoRepository.guardar(punto.copy(codigoQr = codigo))
                _estado.update { it.copy(guardando = false, guardado = true) }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(guardando = false, mensajeError = "No se pudo guardar el punto, intenta de nuevo")
                }
            }
        }
    }

    companion object {
        //La Moneda, igual que el mapa cuando no hay ubicación (M-06)
        private val CENTRO_POR_DEFECTO = Ubicacion(latitud = -33.44294, longitud = -70.65388, altitud = 0.0)

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
