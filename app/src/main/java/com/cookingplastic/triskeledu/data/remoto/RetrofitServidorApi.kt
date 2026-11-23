package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.sincronizacion.RespuestaRegistro
import com.cookingplastic.triskeledu.data.sincronizacion.ServidorApi

//SIM-03 hecho realidad: la misma interfaz que implementaba ServidorSimulado, ahora contra ms-progreso.
//El servidor revisa lo mismo que ReglaSincronizacion (lugar existente, no repetido y a 100 m) y calcula los puntos.
//El usuarioId no se envía: el servidor lo saca del token, así nadie puede sumar puntos a otra cuenta (BKD-05)
class RetrofitServidorApi(private val api: BackendApi) : ServidorApi {

    override suspend fun confirmarEscaneos(usuarioId: String, escaneos: List<ProgresoPunto>): List<RespuestaRegistro> =
        llamar { api.sincronizarEscaneos(escaneos.map(EscaneoDto::desde)) }.map { it.aRespuesta() }

    override suspend fun confirmarComentarios(usuarioId: String, comentarios: List<Comentario>): List<RespuestaRegistro> =
        llamar { api.sincronizarComentarios(comentarios.map(ComentarioDto::desde)) }.map { it.aRespuesta() }
}
