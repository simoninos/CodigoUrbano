package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.ui.event.PerfilUiEvent
import com.cookingplastic.triskeledu.ui.state.CampoPerfil
import com.cookingplastic.triskeledu.ui.state.PerfilUiState
import com.cookingplastic.triskeledu.ui.validation.ReglaNivel
import com.cookingplastic.triskeledu.ui.validation.Validaciones
import com.cookingplastic.triskeledu.ui.validation.primerCampoConError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//P15 · Mi perfil (PER-02): muestra los datos de la cuenta y permite cambiar el nombre de usuario (mismas reglas
//del registro, único) y la contraseña (la actual, la nueva y su repetición). Los errores siguen RN-18
class PerfilViewModel(
    private val usuarioRepository: UsuarioRepository,
    progresoRepository: ProgresoRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(PerfilUiState())
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    private var usuarioId: String? = null

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        val usuarioYPuntos = usuarioRepository.usuarioActual().filterNotNull().flatMapLatest { usuario ->
            if (usuario.rol == RolUsuario.ADMINISTRADOR) flowOf(usuario to null)
            else progresoRepository.totalPuntos(usuario.id).map { usuario to it }
        }
        viewModelScope.launch {
            usuarioYPuntos.collect { (usuario, puntos) -> mostrar(usuario, puntos) }
        }
    }

    //al cambiar el nombre, el usuario conectado vuelve a llegar: lo escrito se reemplaza por lo guardado
    private fun mostrar(usuario: Usuario, puntos: Int?) {
        val primeraVez = usuarioId == null
        usuarioId = usuario.id
        _estado.update {
            it.copy(
                usernameGuardado = usuario.username,
                username = if (primeraVez || !it.cambiaUsername) usuario.username else it.username,
                correo = usuario.correo,
                rol = usuario.rol,
                creadoEn = usuario.creadoEn,
                nivel = puntos?.let { p -> ReglaNivel.nivelDesde(p) },
                cargando = false
            )
        }
    }

    fun onEvent(evento: PerfilUiEvent) {
        when (evento) {
            //si el campo ya mostraba un error, se revisa al escribir para que desaparezca apenas se corrige
            is PerfilUiEvent.OnUsernameChange -> _estado.update {
                it.copy(username = evento.valor, errorUsername = it.errorUsername?.let { Validaciones.validarUsername(evento.valor.trim()) })
            }
            is PerfilUiEvent.OnClaveActualChange -> _estado.update { it.copy(claveActual = evento.valor, errorClaveActual = null) }
            is PerfilUiEvent.OnClaveNuevaChange -> _estado.update {
                it.copy(claveNueva = evento.valor, errorClaveNueva = it.errorClaveNueva?.let { Validaciones.validarClaveRegistro(evento.valor) })
            }
            is PerfilUiEvent.OnRepetirClaveChange -> _estado.update {
                it.copy(repetirClave = evento.valor, errorRepetir = it.errorRepetir?.let { _ -> Validaciones.validarRepetirClave(it.claveNueva, evento.valor) })
            }
            PerfilUiEvent.OnGuardar -> guardar()
            PerfilUiEvent.OnMensajeMostrado -> _estado.update { it.copy(mensaje = null) }
        }
    }

    private fun guardar() {
        val actual = _estado.value
        val id = usuarioId
        if (actual.guardando || id == null) return
        if (!actual.hayCambios) {
            _estado.update { it.copy(mensaje = "No hay cambios para guardar") }
            return
        }
        //RN-18: se revisa todo y el foco va al primer campo con error
        val errorUsername = if (actual.cambiaUsername) Validaciones.validarUsername(actual.username.trim()) else null
        val errorActual = if (actual.cambiaClave && actual.claveActual.isEmpty()) "Ingresa tu contraseña actual" else null
        val errorNueva = if (actual.cambiaClave) Validaciones.validarClaveRegistro(actual.claveNueva) else null
        val errorRepetir = if (actual.cambiaClave) Validaciones.validarRepetirClave(actual.claveNueva, actual.repetirClave) else null
        val campo = primerCampoConError(
            CampoPerfil.USERNAME to errorUsername,
            CampoPerfil.CLAVE_ACTUAL to errorActual,
            CampoPerfil.CLAVE_NUEVA to errorNueva,
            CampoPerfil.REPETIR to errorRepetir
        )
        if (campo != null) {
            _estado.update {
                it.copy(
                    errorUsername = errorUsername, errorClaveActual = errorActual, errorClaveNueva = errorNueva,
                    errorRepetir = errorRepetir, campoConError = campo, intentoEnvio = it.intentoEnvio + 1
                )
            }
            return
        }
        _estado.update { it.copy(guardando = true, mensaje = null) }
        viewModelScope.launch {
            try {
                val nuevo = actual.username.trim()
                //primero se revisa todo lo que puede fallar; recién entonces se cambia algo
                if (actual.cambiaUsername && usuarioRepository.existeUsername(nuevo)) {
                    falla(CampoPerfil.USERNAME) { it.copy(errorUsername = YA_EN_USO) }
                    return@launch
                }
                if (actual.cambiaClave && !usuarioRepository.cambiarClave(id, actual.claveActual, actual.claveNueva)) {
                    falla(CampoPerfil.CLAVE_ACTUAL) { it.copy(errorClaveActual = CLAVE_INCORRECTA) }
                    return@launch
                }
                if (actual.cambiaUsername && !usuarioRepository.cambiarUsername(id, nuevo)) {
                    falla(CampoPerfil.USERNAME) { it.copy(errorUsername = YA_EN_USO) }
                    return@launch
                }
                _estado.update {
                    it.copy(
                        guardando = false, claveActual = "", claveNueva = "", repetirClave = "",
                        errorUsername = null, errorClaveActual = null, errorClaveNueva = null, errorRepetir = null,
                        mensaje = "Cambios guardados"
                    )
                }
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false, mensaje = "No se pudieron guardar los cambios") }
            }
        }
    }

    private fun falla(campo: CampoPerfil, cambio: (PerfilUiState) -> PerfilUiState) {
        _estado.update { cambio(it).copy(guardando = false, campoConError = campo, intentoEnvio = it.intentoEnvio + 1) }
    }

    companion object {
        const val YA_EN_USO = "Ese nombre ya está en uso"
        const val CLAVE_INCORRECTA = "La contraseña actual no es correcta"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                PerfilViewModel(app.container.usuarioRepository, RepositoriosProgreso.progreso(app))
            }
        }
    }
}
