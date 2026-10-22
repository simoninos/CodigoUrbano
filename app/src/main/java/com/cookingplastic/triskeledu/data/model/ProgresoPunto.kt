package com.cookingplastic.triskeledu.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

//Puntos que entrega cada actividad de un lugar. El máximo de un lugar es la suma: 5 + 15 = 20
const val PUNTOS_ESCANEO = 5
const val PUNTOS_VIDEO = 15
const val PUNTOS_MAXIMOS_LUGAR = 20

//Una fila por usuario y lugar, que se crea al escanear el QR.
//Si no existe la fila, el lugar sigue bloqueado para ese usuario.
//El índice único impide que el mismo usuario tenga dos filas del mismo lugar (no se suman 5 puntos dos veces)
@Entity(
    tableName = "progreso_puntos",
    indices = [Index(value = ["usuarioId", "puntoId"], unique = true)]
)
data class ProgresoPunto(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val usuarioId: String,
    val puntoId: String,
    val escaneadoEn: Long = System.currentTimeMillis(),
    //Embedded guarda la ubicación como tres columnas: escaneo_latitud, escaneo_longitud y escaneo_altitud
    @Embedded(prefix = "escaneo_")
    val escaneoUbicacion: Ubicacion,
    val puntosEscaneo: Int = PUNTOS_ESCANEO,
    val videoVistoEn: Long? = null,
    val puntosVideo: Int = 0,
    val selfieUri: String? = null,
    val selfieEn: Long? = null,
    val puntosSelfie: Int = 0,
    //false hasta que exista el backend (evaluación 3)
    val sincronizado: Boolean = false
)

//El estado y el avance no se guardan: se calculan con estas dos funciones.
//Así Simon (Mapa y Detalle) y Mis lugares cuentan los puntos de la misma forma.
fun ProgresoPunto.puntosTotales(): Int = puntosEscaneo + puntosVideo + puntosSelfie

fun ProgresoPunto.estaCompletado(): Boolean = puntosTotales() >= PUNTOS_MAXIMOS_LUGAR