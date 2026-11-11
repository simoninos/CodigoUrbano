package com.cookingplastic.triskeledu.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.RepositoriosProgreso
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.domain.repository.ProgresoRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import com.cookingplastic.triskeledu.data.simulacion.SolicitudesRepository
import com.cookingplastic.triskeledu.domain.rules.ReglaNivel
import com.cookingplastic.triskeledu.domain.rules.Validaciones
import com.cookingplastic.triskeledu.domain.rules.primerCampoConError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//aquí empezamos a ver
//Recibe a usuarioRepository que esté en el momento (Hoy Fake, próximamente el de Room)
// poner que sea ViewModel() permite que la pantalla se adapte y que no se reinicie de 0
class SesionViewModel(
    private val usuarioRepository: UsuarioRepository,
    //ACC-08: el nivel de cada usuario de prueba sale de su progreso
    progresoRepository: ProgresoRepository,
    //RN-29: al entrar, avisa si un administrador respondió su solicitud de creador
    private val solicitudesRepository: SolicitudesRepository? = null
) : ViewModel() {

    //_estado privado pero mutable para que así solo ViewModel escriba
    private val _estado = MutableStateFlow(SesionUiState())
    //Arriba hay un SesionUiState que el que se construyó anteriormente vacío como molde para la pantalla
//estado es público y de solo lectura, así la pantalla lo ve pero no cambia nada
    val estado: StateFlow<SesionUiState> = _estado.asStateFlow()

   //se ejecuta solo una vez e inicia una tarea de segundo plano
    init {
        viewModelScope.launch {      usuarioRepository.usuarioActual().collect { usuario ->
                _estado.update { it.copy(sesionIniciada = usuario != null, sesionComprobada = true) } //collect queda escuchando el Flow del repositorio y también si es que la sesion ya estaba iniciada
            }
        }
        //una fila por usuario de prueba con su nivel; se actualiza si alguien suma puntos
        viewModelScope.launch {
            val usuarios = DatosPrueba.usuarios
            combine(usuarios.map { progresoRepository.totalPuntos(it.id) }) { puntos ->
                usuarios.mapIndexed { i, u ->
                    val enRevision = u.solicitudCreador == EstadoSolicitud.PENDIENTE
                    UsuarioDemo(
                        username = u.username,
                        correo = u.correo,
                        rol = u.rol,
                        //el administrador no tiene nivel (LVL-05)
                        nivel = if (enRevision || u.rol == RolUsuario.ADMINISTRADOR) null else ReglaNivel.numeroDesde(puntos[i]),
                        enRevision = enRevision
                    )
                }
            }.collect { demo -> _estado.update { it.copy(usuariosDemo = demo) } }
        }
    }

   //ahora lo que hay en sesionUiEvent se escribirá aquí
    fun onEvent(evento: SesionUiEvent) {
        when (evento) {
            is SesionUiEvent.OnCorreoChange -> _estado.update {
                it.copy(
                    correo = evento.valor,
                    errorCorreo = Validaciones.validarCorreo(evento.valor.trim()),
                    mensajeError = null
                )
            }
            is SesionUiEvent.OnClaveChange -> _estado.update {
                it.copy(
                    clave = evento.valor,
                    errorClave = Validaciones.validarClaveLogin(evento.valor),
                    mensajeError = null
                )
            }
            SesionUiEvent.OnIniciarSesion -> iniciarSesion()
            //solo rellena: el usuario igual toca "Iniciar sesión"
            is SesionUiEvent.OnAccesoRapido -> _estado.update {
                it.copy(
                    correo = evento.correo,
                    clave = DatosPrueba.CLAVE_DE_PRUEBA,
                    errorCorreo = null,
                    errorClave = null,
                    mensajeError = null
                )
            }
            SesionUiEvent.OnSolicitudPendienteVista -> _estado.update { it.copy(solicitudPendiente = false) }
        }
    }

    private fun iniciarSesion() {
        //toma el estado de ese momemto y hace las comparaciones
        val actual = _estado.value
        if (actual.cargando) return

        //RN-18: se revisan los dos campos aunque no se hayan tocado; con errores no se envía
        val errorCorreo = Validaciones.validarCorreo(actual.correo.trim())
        val errorClave = Validaciones.validarClaveLogin(actual.clave)
        val primero = primerCampoConError(CampoSesion.CORREO to errorCorreo, CampoSesion.CLAVE to errorClave)
        if (primero != null) {
            _estado.update {
                it.copy(errorCorreo = errorCorreo, errorClave = errorClave, campoConError = primero, intentoEnvio = it.intentoEnvio + 1)
            }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, mensajeError = null) }
            try {
                val usuario = usuarioRepository.iniciarSesion(actual.correo.trim().lowercase(), actual.clave)
                _estado.update {
                    it.copy(
                        cargando = false,
                        mensajeError = if (usuario == null) "Correo o contraseña incorrectos" else null,
                        //RN-29: la cuenta en revisión no entra; se muestra P23
                        solicitudPendiente = usuario?.solicitudCreador == EstadoSolicitud.PENDIENTE
                    )
                }
                //SIM-02: la app consulta su solicitud al entrar y muestra la respuesta como notificación
                if (usuario != null && usuario.solicitudResueltaEn != null) avisarRespuesta(usuario.id)
            } catch (e: Exception) {
                _estado.update {
                    it.copy(cargando = false, mensajeError = "No se pudo iniciar sesión, intenta de nuevo")
                }
            }
        }
    }

    //Crea los ViewModel pero...
    //si el aviso falla, la respuesta igual queda en el historial de notificaciones
    private fun avisarRespuesta(usuarioId: String) {
        val solicitudes = solicitudesRepository ?: return
        viewModelScope.launch {
            try {
                solicitudes.avisarRespuesta(usuarioId)
            } catch (e: Exception) {
                //nada que mostrar en el login
            }
        }
    }

    companion object {
        //con factory le damos el relleno de los constructores vacíos
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                SesionViewModel(app.container.usuarioRepository, RepositoriosProgreso.progreso(app), app.container.solicitudesRepository)
            }
        }
    }
}
