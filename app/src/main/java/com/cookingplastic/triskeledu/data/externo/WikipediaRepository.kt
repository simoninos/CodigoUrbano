package com.cookingplastic.triskeledu.data.externo

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Lo que muestra el Detalle bajo "Desde Wikipedia"
data class ResumenLugar(
    val titulo: String,
    val extracto: String,
    val imagenUrl: String?
)

interface WikipediaRepository {
    // null si el enlace no es de Wikipedia o el artículo no tiene resumen. Lanza excepción si falla la red
    suspend fun resumen(enlace: String): ResumenLugar?
}

// Para pantallas y pruebas que no necesitan Wikipedia
object SinWikipedia : WikipediaRepository {
    override suspend fun resumen(enlace: String): ResumenLugar? = null
}

// Convierte el enlace "Descubre más" en la URL del resumen. Sin Android: se prueba con JUnit
object EnlaceWikipedia {

    private val articulo = Regex("""^https?://([a-z]{2,3}(?:\.m)?)\.wikipedia\.org/wiki/([^?#]+)""")

    // "https://es.wikipedia.org/wiki/Palacio_de_La_Moneda"
    //   -> "https://es.wikipedia.org/api/rest_v1/page/summary/Palacio_de_La_Moneda"
    fun urlResumen(enlace: String?): String? {
        val coincidencia = articulo.find(enlace?.trim().orEmpty()) ?: return null
        val idioma = coincidencia.groupValues[1].removeSuffix(".m")
        val titulo = coincidencia.groupValues[2].trimEnd('/')
        if (titulo.isBlank()) return null
        return "https://$idioma.wikipedia.org/api/rest_v1/page/summary/$titulo"
    }
}

class RetrofitWikipediaRepository(private val api: WikipediaApi) : WikipediaRepository {

    override suspend fun resumen(enlace: String): ResumenLugar? {
        val url = EnlaceWikipedia.urlResumen(enlace) ?: return null
        val dto = api.resumen(url)
        // "disambiguation" es una página de desambiguación: no sirve como resumen del lugar
        if (dto.tipo == "disambiguation") return null
        val extracto = dto.extracto?.trim().orEmpty()
        if (extracto.isEmpty()) return null
        return ResumenLugar(titulo = dto.titulo.orEmpty(), extracto = extracto, imagenUrl = dto.miniatura?.url)
    }

    companion object {
        // EXC-06: si Wikipedia no responde en 10 s, se corta y el Detalle sigue sin el resumen
        fun crear(): RetrofitWikipediaRepository {
            val cliente = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                // Wikipedia pide identificar la app en el User-Agent
                .addInterceptor { cadena ->
                    cadena.proceed(
                        cadena.request().newBuilder()
                            .header("User-Agent", "AprendoIA-Triskeledu/1.0 (proyecto academico Android)")
                            .build()
                    )
                }
                .build()
            val api = Retrofit.Builder()
                // obligatoria para Retrofit aunque cada llamada use su URL completa
                .baseUrl("https://es.wikipedia.org/")
                .client(cliente)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WikipediaApi::class.java)
            return RetrofitWikipediaRepository(api)
        }
    }
}
