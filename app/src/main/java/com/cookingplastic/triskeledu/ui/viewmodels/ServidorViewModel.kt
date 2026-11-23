package com.cookingplastic.triskeledu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.remoto.DireccionServidor
import com.cookingplastic.triskeledu.ui.event.ServidorUiEvent
import com.cookingplastic.triskeledu.ui.state.ServidorUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl

//P01, "Servidor": cambia la dirección del gateway sin reinstalar la app. En la demo el celular es el punto de
//acceso y el computador tiene todo el backend en Docker: aquí se escribe la IP del computador en esa red
class ServidorViewModel(
    private val direccion: DireccionServidor,
    //se puede reemplazar en las pruebas; en la app pregunta /actuator/health al gateway
    private val probar: suspend (HttpUrl) -> Boolean = { DireccionServidor.responde(it) }
) : ViewModel() {

    private val _estado = MutableStateFlow(ServidorUiState(deFabrica = mostrar(direccion.deFabrica)))
    val estado: StateFlow<ServidorUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            direccion.actual.collect { url -> _estado.update { it.copy(actual = mostrar(url)) } }
        }
    }

    fun onEvent(evento: ServidorUiEvent) {
        when (evento) {
            ServidorUiEvent.OnAbrir -> _estado.update {
                it.copy(dialogoAbierto = true, texto = it.actual, error = null, responde = null)
            }
            ServidorUiEvent.OnCerrar -> _estado.update { it.copy(dialogoAbierto = false, probando = false) }
            is ServidorUiEvent.OnTextoChange -> _estado.update { it.copy(texto = evento.texto, error = null, responde = null) }
            ServidorUiEvent.OnGuardar -> guardar()
            ServidorUiEvent.OnRestablecer -> {
                direccion.restablecer()
                _estado.update { it.copy(texto = it.deFabrica, error = null, responde = null) }
            }
        }
    }

    private fun guardar() {
        val url = DireccionServidor.normalizar(_estado.value.texto)
        if (url == null) {
            _estado.update { it.copy(error = "Escribe una dirección como 192.168.43.10 o 192.168.43.10:8080") }
            return
        }
        //se guarda aunque no responda: el backend puede estar encendiéndose
        direccion.cambiar(url)
        _estado.update { it.copy(texto = mostrar(url), error = null, probando = true, responde = null) }
        viewModelScope.launch {
            val responde = probar(url)
            _estado.update { it.copy(probando = false, responde = responde) }
        }
    }

    companion object {
        //sin la barra final, que no aporta al leerla
        fun mostrar(url: HttpUrl): String = url.toString().removeSuffix("/")

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                ServidorViewModel(app.container.direccionServidor)
            }
        }
    }
}
