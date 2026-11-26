package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.security.ClaveDispositivo
import com.cookingplastic.triskeledu.data.security.textoFirmado
import com.cookingplastic.triskeledu.data.sincronizacion.RespuestaRegistro
import com.cookingplastic.triskeledu.data.sincronizacion.ServidorApi

//SIM-03 hecho realidad: la misma interfaz que implementaba ServidorSimulado, ahora contra ms-progreso.
//El servidor revisa lo mismo que ReglaSincronizacion (lugar existente, no repetido y a 100 m) y calcula los puntos.
//El usuarioId no se envía: el servidor lo saca del token, así nadie puede sumar puntos a otra cuenta (BKD-05)
//SEG-05: cada escaneo va firmado con la clave del teléfono que se verificó al iniciar sesión (ClaveDispositivo).
//Se firma al enviarlo, con la clave del inicio de sesión actual, que es la que viaja en el token
class RetrofitServidorApi(
    private val api: BackendApi,
    private val clave: ClaveDispositivo? = null
) : ServidorApi {

    override suspend fun confirmarEscaneos(usuarioId: String, escaneos: List<ProgresoPunto>): List<RespuestaRegistro> =
        llamar { api.sincronizarEscaneos(escaneos.map { EscaneoDto.desde(it, clave?.firmar(textoFirmado(it))) }) }
            .map { it.aRespuesta() }

    override suspend fun confirmarComentarios(usuarioId: String, comentarios: List<Comentario>): List<RespuestaRegistro> =
        llamar { api.sincronizarComentarios(comentarios.map(ComentarioDto::desde)) }.map { it.aRespuesta() }
}
