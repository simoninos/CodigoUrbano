package com.cookingplastic.triskeledu.data.remoto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

//BKD-01: la dirección del API Gateway. En la demo el celular es el punto de acceso y el computador (con todo el
//backend en Docker) se conecta a su red: la IP del computador la asigna el celular y puede cambiar, así que se
//escribe desde la app (P01, "Servidor") y queda guardada. Sin nada guardado se usa la de BuildConfig.
//leer y guardar son lambdas para probarla sin Android (en la app, SharedPreferences)
class DireccionServidor(
    porDefecto: String,
    private val leer: () -> String?,
    private val guardar: (String?) -> Unit
) {
    val deFabrica: HttpUrl = requireNotNull(normalizar(porDefecto)) { "URL_BACKEND no es una dirección válida" }

    private val _actual = MutableStateFlow(leer()?.let(::normalizar) ?: deFabrica)
    val actual: StateFlow<HttpUrl> = _actual.asStateFlow()

    fun cambiar(url: HttpUrl) {
        guardar(url.toString())
        _actual.value = url
    }

    fun restablecer() {
        guardar(null)
        _actual.value = deFabrica
    }

    companion object {
        const val PUERTO_GATEWAY = 8080

        //Acepta lo que alguien escribiría en la demo: "192.168.43.10", "192.168.43.10:8080" o
        //"http://192.168.43.10:8080/". Sin puerto se usa el del gateway. Devuelve null si no es una dirección
        fun normalizar(texto: String): HttpUrl? {
            val limpio = texto.trim()
            if (limpio.isEmpty() || limpio.any { it.isWhitespace() }) return null
            val conEsquema = if ("://" in limpio) limpio else "http://$limpio"
            val url = conEsquema.toHttpUrlOrNull() ?: return null
            if (url.host.isBlank()) return null
            //HttpUrl pone 80 si no viene el puerto: se mira el texto para saber si lo escribieron
            val anfitrion = conEsquema.substringAfter("://").substringBefore('/')
            val traePuerto = Regex(""":\d+$""").containsMatchIn(anfitrion)
            return url.newBuilder()
                .apply { if (!traePuerto) port(PUERTO_GATEWAY) }
                .encodedPath("/")
                .query(null)
                .fragment(null)
                .build()
        }

        //Pregunta /actuator/health al gateway, con poco tiempo de espera: para el botón "Probar" de P01
        suspend fun responde(url: HttpUrl, cliente: OkHttpClient = clienteRapido): Boolean = withContext(Dispatchers.IO) {
            runCatching {
                cliente.newCall(Request.Builder().url(url.resolve("actuator/health")!!).build()).execute().use { it.isSuccessful }
            }.getOrDefault(false)
        }

        private val clienteRapido by lazy {
            OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).build()
        }
    }
}

//Cambia el esquema, la IP y el puerto de cada petición por los de la dirección guardada en ese momento.
//Así la dirección se cambia sin reiniciar la app ni volver a crear Retrofit
class InterceptorServidor(private val direccion: () -> HttpUrl) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val base = direccion()
        val pedida = chain.request()
        val url = pedida.url.newBuilder().scheme(base.scheme).host(base.host).port(base.port).build()
        return chain.proceed(pedida.newBuilder().url(url).build())
    }
}
