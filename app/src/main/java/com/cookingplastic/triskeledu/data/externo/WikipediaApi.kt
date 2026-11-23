package com.cookingplastic.triskeledu.data.externo

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Url

// API externa (EP4): el resumen de Wikipedia de cada lugar, el mismo artículo que abre "Descubre más".
// Endpoint público, sin clave: GET https://es.wikipedia.org/api/rest_v1/page/summary/{título}
interface WikipediaApi {

    // Se pasa la URL completa porque el idioma (es, en...) sale del enlace guardado en el punto
    @GET
    suspend fun resumen(@Url url: String): ResumenWikipediaDto
}

// Solo los campos que usa la app; Gson ignora el resto de la respuesta
data class ResumenWikipediaDto(
    @SerializedName("title") val titulo: String?,
    @SerializedName("extract") val extracto: String?,
    @SerializedName("thumbnail") val miniatura: ImagenDto?,
    @SerializedName("type") val tipo: String?
)

data class ImagenDto(
    @SerializedName("source") val url: String?
)
