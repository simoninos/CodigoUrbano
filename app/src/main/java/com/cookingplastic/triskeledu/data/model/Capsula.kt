package com.cookingplastic.triskeledu.data.model

import java.util.UUID
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cookingplastic.triskeledu.data.local.DatosPrueba

//v3: la cápsula tiene creador, estado y fechas. El creador es un usuario (clave foránea con índice)
@Entity(
    tableName = "capsulas",
    foreignKeys = [ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["creadorId"])],
    indices = [Index(value = ["creadorId"])]
)
data class Capsula(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val descripcion: String? = null,
    //enlace de Drive (RN-26); null = sin enlace, solo en los datos de prueba (RN-35)
    val urlStream: String?,
    val categoria: CategoriaCapsula,
    val duracionSegundos: Int,
    val estado: EstadoCapsula = EstadoCapsula.BORRADOR,
    val creadorId: String,
    val creadaEn: Long = System.currentTimeMillis(),
    //se llena al publicar y vuelve a null al retirar la publicación
    val publicadaEn: Long? = null
)

fun Capsula.tieneEnlace(): Boolean = !urlStream.isNullOrBlank()

//RN-14: para activar un punto, su cápsula debe estar PUBLICADA y tener enlace.
//Excepción de demo: las cápsulas de los datos de prueba sirven aunque no tengan enlace (RN-35)
fun Capsula.sirveParaActivar(): Boolean =
    estado == EstadoCapsula.PUBLICADA && (tieneEnlace() || id in DatosPrueba.idsCapsulasPrueba)
