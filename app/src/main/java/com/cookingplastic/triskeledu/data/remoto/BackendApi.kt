package com.cookingplastic.triskeledu.data.remoto

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

//Backend propio (BKD-01): todas las rutas pasan por el API Gateway (puerto 8080), que las reenvía
//a ms-usuarios, ms-contenido o ms-progreso. El token va en la cabecera Authorization (ClienteBackend)
interface BackendApi {

    // ---- ms-usuarios (BKD-02) ----
    @POST("api/auth/registro")
    suspend fun registrar(@Body cuerpo: RegistroDto): SesionDto

    @POST("api/auth/login")
    suspend fun iniciarSesion(@Body cuerpo: LoginDto): SesionDto

    @GET("api/solicitudes")
    suspend fun solicitudesPendientes(): List<UsuarioDto>

    @POST("api/solicitudes/{usuarioId}/aprobar")
    suspend fun aprobarSolicitud(@Path("usuarioId") usuarioId: String): UsuarioDto

    @POST("api/solicitudes/{usuarioId}/rechazar")
    suspend fun rechazarSolicitud(@Path("usuarioId") usuarioId: String): UsuarioDto

    // ---- ms-contenido (BKD-03) ----
    @GET("api/puntos")
    suspend fun puntos(): List<PuntoDto>

    @POST("api/puntos")
    suspend fun crearPunto(@Body cuerpo: PuntoEnvioDto): PuntoDto

    @PUT("api/puntos/{id}")
    suspend fun editarPunto(@Path("id") id: String, @Body cuerpo: PuntoEnvioDto): PuntoDto

    @PATCH("api/puntos/{id}/activo")
    suspend fun cambiarActivo(@Path("id") id: String, @Body cuerpo: ActivoDto): PuntoDto

    //CRE-08: el creador asocia su cápsula; no puede editar el resto del punto
    @PUT("api/puntos/{id}/capsula")
    suspend fun asociarCapsula(@Path("id") id: String, @Body cuerpo: AsociarDto): PuntoDto

    //Response<Unit>: el 204 no trae cuerpo
    @DELETE("api/puntos/{id}")
    suspend fun eliminarPunto(@Path("id") id: String): Response<Unit>

    @GET("api/capsulas")
    suspend fun capsulas(): List<CapsulaDto>

    @POST("api/capsulas")
    suspend fun crearCapsula(@Body cuerpo: CapsulaEnvioDto): CapsulaDto

    @PUT("api/capsulas/{id}")
    suspend fun editarCapsula(@Path("id") id: String, @Body cuerpo: CapsulaEnvioDto): CapsulaDto

    @POST("api/capsulas/{id}/publicar")
    suspend fun publicarCapsula(@Path("id") id: String): CapsulaDto

    @POST("api/capsulas/{id}/retirar")
    suspend fun retirarCapsula(@Path("id") id: String): CapsulaDto

    @DELETE("api/capsulas/{id}")
    suspend fun eliminarCapsula(@Path("id") id: String): Response<Unit>

    // ---- ms-progreso (BKD-04) ----
    @POST("api/escaneos/sincronizar")
    suspend fun sincronizarEscaneos(@Body cuerpo: List<EscaneoDto>): List<RespuestaRegistroDto>

    @POST("api/comentarios/sincronizar")
    suspend fun sincronizarComentarios(@Body cuerpo: List<ComentarioDto>): List<RespuestaRegistroDto>

    //LUG-06: periodo = MES o SIEMPRE
    @GET("api/ranking")
    suspend fun ranking(@Query("periodo") periodo: String): RankingDto
}
