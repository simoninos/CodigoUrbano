package com.cookingplastic.triskeledu.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

//Puntos que entrega cada actividad de un lugar (RN-01 a RN-03). El máximo de un lugar es la suma: 10 + 50 + 20 = 80
const val PUNTOS_ESCANEO = 10
const val PUNTOS_CAPSULA = 50
const val PUNTOS_SELFIE = 20
const val PUNTOS_MAXIMOS_LUGAR = PUNTOS_ESCANEO + PUNTOS_CAPSULA + PUNTOS_SELFIE

//Una fila por usuario y lugar, que se crea al escanear el QR.
//Si no existe la fila, el lugar sigue bloqueado para ese usuario.
//El índice único impide que el mismo usuario tenga dos filas del mismo lugar (no se suman 10 puntos dos veces)
//v3: clave foránea a puntos_interes con RESTRICT: la base no deja eliminar un punto que alguien escaneó (RN-30)
@Entity(
    tableName = "progreso_puntos",
    indices = [
        Index(value = ["usuarioId", "puntoId"], unique = true),
        Index(value = ["puntoId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PuntoInteres::class,
            parentColumns = ["id"],
            childColumns = ["puntoId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
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
    //se llena al terminar la cápsula; también decide si el reproductor va bloqueado o libre (RN-08)
    val capsulaVistaEn: Long? = null,
    val puntosCapsula: Int = 0,
    val selfieUri: String? = null,
    val selfieEn: Long? = null,
    val puntosSelfie: Int = 0,
    //false hasta que el servidor confirme el escaneo
    val sincronizado: Boolean = false
)

//El estado y el avance no se guardan: se calculan con estas dos funciones.
//Así Simon (Mapa y Detalle) y Mis lugares cuentan los puntos de la misma forma.
fun ProgresoPunto.puntosTotales(): Int = puntosEscaneo + puntosCapsula + puntosSelfie

fun ProgresoPunto.estaCompletado(): Boolean = puntosTotales() >= PUNTOS_MAXIMOS_LUGAR
