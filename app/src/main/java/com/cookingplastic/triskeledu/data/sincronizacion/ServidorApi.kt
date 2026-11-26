package com.cookingplastic.triskeledu.data.sincronizacion

import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.ProgresoPunto

//SIM-03: lo que la app le pide al servidor al sincronizar. Hoy lo responde ServidorSimulado (data/simulacion);
//cuando exista el backend, una versión con Retrofit implementa esta misma interfaz y solo cambia AppContainer.
//Ni los ViewModels ni el Sincronizador se tocan
interface ServidorApi {
    //ESC-02: revalida escaneos hechos en el teléfono. Devuelve una respuesta por cada fila enviada
    suspend fun confirmarEscaneos(usuarioId: String, escaneos: List<ProgresoPunto>): List<RespuestaRegistro>

    //BKD-06: los comentarios escritos sin conexión
    suspend fun confirmarComentarios(usuarioId: String, comentarios: List<Comentario>): List<RespuestaRegistro>
}

//Respuesta del servidor para un registro: aceptado, o rechazado con el motivo
data class RespuestaRegistro(
    val id: String,
    val motivo: MotivoRechazo? = null
) {
    val aceptado: Boolean get() = motivo == null
}

//EXC-08: por qué el servidor no aceptó un registro
enum class MotivoRechazo(val explicacion: String) {
    LEJOS("El escaneo quedó a más de 100 m del lugar"),
    DUPLICADO("Ese lugar ya estaba registrado en tu cuenta"),
    LUGAR_DESCONOCIDO("El lugar ya no existe"),
    TEXTO_INVALIDO("El comentario no cumple las reglas"),
    //SEG-01 a SEG-05
    UBICACION_SIMULADA("La ubicación del escaneo era simulada"),
    VELOCIDAD_IMPOSIBLE("Hubo dos escaneos demasiado lejos en muy poco tiempo"),
    QR_INVALIDO("El código QR no es válido"),
    APP_NO_VERIFICADA("No se pudo verificar que la app sea la original")
}
