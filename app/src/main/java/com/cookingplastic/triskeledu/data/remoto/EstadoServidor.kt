package com.cookingplastic.triskeledu.data.remoto

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

//EXC-06: si el servidor responde o no. Lo actualiza cada llamada al backend (InterceptorEstado) y, mientras una
//pantalla de edición está abierta, una consulta a /actuator/health cada pocos segundos. Las pantallas del
//administrador y del creador lo usan para avisar y no dejar editar sin conexión
class EstadoServidor(
    private val probar: suspend () -> Boolean,
    private val cadaMs: Long = 10_000
) {
    //se parte suponiendo que hay conexión, para no mostrar el aviso antes de la primera respuesta
    private val _conectado = MutableStateFlow(true)
    val conectado: StateFlow<Boolean> = _conectado.asStateFlow()

    fun informar(respondio: Boolean) {
        _conectado.value = respondio
    }

    suspend fun revisarAhora(): Boolean = probar().also { _conectado.value = it }

    //lanza SinServidor si el servidor no responde; para las escrituras que no llaman al servidor
    suspend fun exigir() {
        if (!revisarAhora()) throw SinServidor(IOException("sin respuesta"), SIN_CONEXION_EDITAR)
    }

    //revisa mientras dure el alcance (el de la pantalla): al cerrarla, se detiene
    fun vigilar(alcance: CoroutineScope): Job = alcance.launch {
        while (isActive) {
            revisarAhora()
            delay(cadaMs)
        }
    }

    companion object {
        const val SIN_CONEXION_EDITAR = "Sin conexión con el servidor: no se puede editar"
    }
}

//Cada respuesta del backend dice que hay conexión; cada error de red, que no
class InterceptorEstado(private val estado: EstadoServidor) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = try {
        chain.proceed(chain.request()).also { estado.informar(true) }
    } catch (e: IOException) {
        estado.informar(false)
        throw e
    }
}
