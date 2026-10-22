package com.cookingplastic.triskeledu.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

//Comentario que deja un usuario en el detalle de un lugar (pantalla 4, Simon)
//El índice en puntoId hace rápida la búsqueda "comentarios de este lugar"
@Entity(
    tableName = "comentarios",
    indices = [Index(value = ["puntoId"])]
)
data class Comentario(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val usuarioId: String,
    val puntoId: String,
    val texto: String,
    val creadoEn: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false
)