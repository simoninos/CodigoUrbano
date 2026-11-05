package com.cookingplastic.triskeledu.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Tabla de los puntos de interés.
// - @Embedded guarda la ubicación en tres columnas: latitud, longitud y altitud.
// - El código QR es único y obligatorio (v3).
// - Clave foránea a Capsula con SET_NULL: si se borra la cápsula, el punto se conserva
//   y su capsulaId queda en null (Sin cápsula).
// - v3: el índice en capsulaId es único: una cápsula pertenece a un solo punto (RN-20).
//   SQLite permite varios null en un índice único, así que los puntos sin cápsula no chocan.
@Entity(
    tableName = "puntos_interes",
    indices = [
        Index(value = ["codigoQr"], unique = true),
        Index(value = ["capsulaId"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = Capsula::class,
            parentColumns = ["id"],
            childColumns = ["capsulaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class PuntoInteres(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val descripcion: String?,
    val tipo: TipoPuntoInteres,
    @Embedded
    val ubicacion: Ubicacion,
    val imagenUri: String,
    val descubreMasUrl: String?,
    val codigoQr: String,
    val capsulaId: String?,
    val activo: Boolean = false,
    val actualizadoEn: Long = System.currentTimeMillis()
)

// RN-14: un punto solo se puede activar si tiene nombre, imagen, QR y una cápsula PUBLICADA con enlace.
// La descripción es opcional. El tipo y la ubicación no aceptan null, por eso no se revisan.
// Recibe la cápsula porque el punto solo guarda su id; null = no tiene o no se encontró
fun PuntoInteres.puedeActivarse(capsula: Capsula?): Boolean = datosFaltantes(capsula).isEmpty()

// Lo que le falta a un punto para poder activarse, en el orden en que se muestra (RF-A09)
fun PuntoInteres.datosFaltantes(capsula: Capsula?): List<String> = buildList {
    if (nombre.isBlank()) add("nombre")
    if (imagenUri.isBlank()) add("imagen")
    if (capsulaId.isNullOrBlank() || capsula == null || capsula.id != capsulaId || !capsula.sirveParaActivar()) add("cápsula")
    if (codigoQr.isBlank()) add("QR")
}
