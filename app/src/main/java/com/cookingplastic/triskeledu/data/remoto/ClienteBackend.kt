package com.cookingplastic.triskeledu.data.remoto

import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

//Error que respondió el backend, con el formato común de los tres servicios:
//{"error": "mensaje", "campos": {"correo": "Ese correo ya está registrado"}}
class ErrorBackend(
    val codigo: Int,
    override val message: String,
    val campos: Map<String, String> = emptyMap()
) : Exception(message)

//EXC-06: no hubo respuesta (servidor apagado, sin red o tiempo agotado)
class SinServidor(causa: Throwable) : IOException("No hay conexión con el servidor. Intenta más tarde", causa)

//Mensaje para mostrar al usuario: el del servidor si lo hay, o el que da la pantalla
fun Throwable.mensajeParaUsuario(porDefecto: String): String = when (this) {
    is ErrorBackend -> message
    is SinServidor -> message ?: porDefecto
    else -> porDefecto
}

private data class CuerpoError(val error: String?, val campos: Map<String, String>?)

//Ejecuta una llamada y deja solo dos tipos de error: ErrorBackend (respondió con 4xx o 5xx) o SinServidor
suspend fun <T> llamar(peticion: suspend () -> T): T = try {
    peticion()
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    throw aErrorBackend(e.code(), e.response()?.errorBody()?.string())
} catch (e: IOException) {
    throw SinServidor(e)
}

//Para los DELETE, que responden 204 sin cuerpo: Retrofit no lanza HttpException, hay que mirar el código
suspend fun llamarSinCuerpo(peticion: suspend () -> retrofit2.Response<Unit>) {
    val respuesta = llamar(peticion)
    if (!respuesta.isSuccessful) throw aErrorBackend(respuesta.code(), respuesta.errorBody()?.string())
}

internal fun aErrorBackend(codigo: Int, cuerpo: String?): ErrorBackend {
    val leido = runCatching { Gson().fromJson(cuerpo, CuerpoError::class.java) }.getOrNull()
    val mensaje = leido?.error ?: when (codigo) {
        401 -> "Tu sesión venció. Vuelve a iniciar sesión"
        403 -> "No tienes permiso para hacer esto"
        404 -> "No se encontró"
        503 -> "El servicio no está disponible; intenta más tarde"
        else -> "El servidor respondió un error ($codigo)"
    }
    return ErrorBackend(codigo, mensaje, leido?.campos.orEmpty())
}

//BKD-05: agrega "Authorization: Bearer <token>" si hay sesión con token. Si el servidor responde 401
//a una petición que llevaba token, el token venció o es inválido (EXC-07): se avisa para cerrar la sesión
class InterceptorToken(
    private val token: () -> String?,
    private val alVencer: () -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val valor = token()
        val peticion = if (valor == null) chain.request()
        else chain.request().newBuilder().header("Authorization", "Bearer $valor").build()
        val respuesta = chain.proceed(peticion)
        if (respuesta.code == 401 && valor != null) alVencer()
        return respuesta
    }
}

object ClienteBackend {
    //tiempo corto: con el servidor apagado, la app avisa rápido en vez de quedarse cargando
    private const val SEGUNDOS = 10L

    //servidor: la dirección guardada en la app (DireccionServidor); si no se pasa, se usa urlBase tal cual
    fun crear(urlBase: String, token: () -> String?, alVencer: () -> Unit, servidor: (() -> HttpUrl)? = null): BackendApi {
        val http = OkHttpClient.Builder()
            .apply { if (servidor != null) addInterceptor(InterceptorServidor(servidor)) }
            .addInterceptor(InterceptorToken(token, alVencer))
            .connectTimeout(SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(SEGUNDOS, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(urlBase)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }
}
