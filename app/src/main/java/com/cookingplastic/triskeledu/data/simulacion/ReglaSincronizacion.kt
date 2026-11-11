package com.cookingplastic.triskeledu.data.simulacion

import com.cookingplastic.triskeledu.data.location.distanciaEnMetros
import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.sincronizacion.MotivoRechazo
import com.cookingplastic.triskeledu.data.sincronizacion.RespuestaRegistro
import com.cookingplastic.triskeledu.domain.rules.RADIO_ESCANEO_METROS
import com.cookingplastic.triskeledu.domain.rules.ValidacionComentario

//SIM-03: las revisiones que haría el backend al recibir lo guardado sin conexión. Solo lógica: se prueban sin Android
object ReglaSincronizacion {

    //ESC-02 y EXC-08: un escaneo pasa si se hizo a 100 m o menos del punto y el usuario no tiene ya ese lugar.
    //confirmados son las filas que el servidor ya aceptó antes (sincronizado = true)
    fun revisarEscaneos(
        pendientes: List<ProgresoPunto>,
        confirmados: List<ProgresoPunto>,
        puntos: Map<String, PuntoInteres>
    ): List<RespuestaRegistro> {
        //usuario y lugar que ya tienen una fila aceptada; se va llenando para detectar duplicados entre los pendientes
        val idsPendientes = pendientes.map { it.id }.toSet()
        val registrados = confirmados.filter { it.id !in idsPendientes }.map { it.usuarioId to it.puntoId }.toMutableSet()
        return pendientes.sortedBy { it.escaneadoEn }.map { escaneo ->
            val punto = puntos[escaneo.puntoId]
            val motivo = when {
                punto == null -> MotivoRechazo.LUGAR_DESCONOCIDO
                (escaneo.usuarioId to escaneo.puntoId) in registrados -> MotivoRechazo.DUPLICADO
                distanciaEnMetros(escaneo.escaneoUbicacion, punto.ubicacion) > RADIO_ESCANEO_METROS -> MotivoRechazo.LEJOS
                else -> null
            }
            if (motivo == null) registrados += escaneo.usuarioId to escaneo.puntoId
            RespuestaRegistro(escaneo.id, motivo)
        }
    }

    //un comentario pasa si cumple RN-18 (1 a 280 caracteres) y su lugar existe
    fun revisarComentarios(pendientes: List<Comentario>, puntos: Map<String, PuntoInteres>): List<RespuestaRegistro> =
        pendientes.map { comentario ->
            val motivo = when {
                puntos[comentario.puntoId] == null -> MotivoRechazo.LUGAR_DESCONOCIDO
                ValidacionComentario.validar(comentario.texto) != null -> MotivoRechazo.TEXTO_INVALIDO
                else -> null
            }
            RespuestaRegistro(comentario.id, motivo)
        }
}
